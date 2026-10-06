package br.com.ricardo.diariodeclasse.data.repository

import br.com.ricardo.diariodeclasse.data.local.dao.ChamadaDao
import br.com.ricardo.diariodeclasse.data.local.entity.Chamada
import br.com.ricardo.diariodeclasse.data.local.entity.FaltaDoAluno
import br.com.ricardo.diariodeclasse.data.local.entity.RegistroPresenca
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** DAO em memória: o upsert substitui a linha de mesmo id ou acrescenta uma nova. */
private class FakeChamadaDao : ChamadaDao {
    val chamadas = MutableStateFlow<List<Chamada>>(emptyList())
    val registros = MutableStateFlow<List<RegistroPresenca>>(emptyList())

    override fun observarChamada(turmaId: String, data: LocalDate): Flow<Chamada?> {
        return chamadas.map { lista ->
            lista.firstOrNull { chamada ->
                chamada.turmaId == turmaId && chamada.data == data && chamada.deletedAt == null
            }
        }
    }

    override fun observarRegistrosDaChamada(turmaId: String, data: LocalDate): Flow<List<RegistroPresenca>> {
        return observarChamada(turmaId, data).map { chamada ->
            if (chamada == null) {
                emptyList()
            } else {
                registros.value.filter { registro -> registro.chamadaId == chamada.id }
            }
        }
    }

    /** Junta registros e chamadas como o JOIN do banco real faria. */
    override fun observarFaltasDoAluno(alunoId: String): Flow<List<FaltaDoAluno>> {
        return registros.map { lista ->
            val faltas = mutableListOf<FaltaDoAluno>()
            for (registro in lista) {
                val chamada: Chamada? = chamadas.value.firstOrNull { chamada -> chamada.id == registro.chamadaId }
                if (chamada != null && registro.alunoId == alunoId && !registro.presente) {
                    faltas.add(FaltaDoAluno(chamada.data, registro.observacao))
                }
            }
            faltas.sortedByDescending { falta -> falta.data }
        }
    }

    override suspend fun salvarChamada(chamada: Chamada) {
        val outras: List<Chamada> = chamadas.value.filter { existente -> existente.id != chamada.id }
        chamadas.value = outras + chamada
    }

    override suspend fun salvarRegistros(registros: List<RegistroPresenca>) {
        for (registro in registros) {
            val outros: List<RegistroPresenca> = this.registros.value.filter { existente -> existente.id != registro.id }
            this.registros.value = outros + registro
        }
    }

    fun registroDoAluno(alunoId: String): RegistroPresenca {
        return registros.value.first { registro -> registro.alunoId == alunoId }
    }
}

class ChamadaRepositoryImplTest {
    private val inicio = Instant.parse("2026-10-06T11:00:00Z")
    private val hoje = LocalDate.of(2026, 10, 6)
    private val dao = FakeChamadaDao()

    private fun repositorioNoInstante(agora: Instant): ChamadaRepositoryImpl {
        return ChamadaRepositoryImpl(dao, Clock.fixed(agora, ZoneOffset.UTC))
    }

    private val anaPresente = MarcacaoPresenca(alunoId = "ana", presente = true, observacao = null)
    private val brunoFaltou = MarcacaoPresenca(alunoId = "bruno", presente = false, observacao = "atestado")

    @Test
    fun semChamadaNoDia_buscarRetornaNull() = runBlocking {
        assertNull(repositorioNoInstante(inicio).buscarChamada("turma", hoje))
    }

    @Test
    fun salvar_criaChamadaComRegistroDeCadaAluno() = runBlocking {
        val repositorio = repositorioNoInstante(inicio)

        repositorio.salvar("turma", hoje, listOf(anaPresente, brunoFaltou))

        val chamadaDoDia = repositorio.buscarChamada("turma", hoje)!!
        assertEquals(inicio, chamadaDoDia.chamada.createdAt)
        assertEquals(2, chamadaDoDia.registros.size)
        assertTrue(dao.registroDoAluno("ana").presente)
        assertFalse(dao.registroDoAluno("bruno").presente)
        assertEquals("atestado", dao.registroDoAluno("bruno").observacao)
    }

    @Test
    fun salvarDuasVezesNoMesmoDia_reaproveitaAChamada() = runBlocking {
        repositorioNoInstante(inicio).salvar("turma", hoje, listOf(anaPresente))
        repositorioNoInstante(inicio.plusSeconds(60)).salvar("turma", hoje, listOf(anaPresente))

        assertEquals(1, dao.chamadas.value.size)
    }

    @Test
    fun outroDia_criaOutraChamada() = runBlocking {
        val repositorio = repositorioNoInstante(inicio)

        repositorio.salvar("turma", hoje, listOf(anaPresente))
        repositorio.salvar("turma", hoje.plusDays(1), listOf(anaPresente))

        assertEquals(2, dao.chamadas.value.size)
    }

    @Test
    fun editar_atualizaUpdatedAtDaChamadaESoDosRegistrosAlterados() = runBlocking {
        repositorioNoInstante(inicio).salvar("turma", hoje, listOf(anaPresente, brunoFaltou))
        val umaHoraDepois = inicio.plusSeconds(3600)

        val anaFaltou = anaPresente.copy(presente = false)
        repositorioNoInstante(umaHoraDepois).salvar("turma", hoje, listOf(anaFaltou, brunoFaltou))

        val chamada = dao.chamadas.value.single()
        assertEquals(inicio, chamada.createdAt)
        assertEquals(umaHoraDepois, chamada.updatedAt)
        assertFalse(dao.registroDoAluno("ana").presente)
        assertEquals(umaHoraDepois, dao.registroDoAluno("ana").updatedAt)
        assertEquals(inicio, dao.registroDoAluno("bruno").updatedAt)
    }

    @Test
    fun alunoNovoNaEdicao_ganhaRegistro() = runBlocking {
        repositorioNoInstante(inicio).salvar("turma", hoje, listOf(anaPresente))

        repositorioNoInstante(inicio).salvar("turma", hoje, listOf(anaPresente, brunoFaltou))

        assertEquals(2, dao.registros.value.size)
    }

    @Test
    fun salvar_devolveAChamadaGravadaComOsIdsDosRegistros() = runBlocking {
        val chamadaSalva = repositorioNoInstante(inicio).salvar("turma", hoje, listOf(anaPresente, brunoFaltou))

        assertEquals(2, chamadaSalva.registros.size)
        assertEquals(dao.registroDoAluno("bruno").id, chamadaSalva.registros.first { registro -> registro.alunoId == "bruno" }.id)
    }

    @Test
    fun observarFaltasDoAluno_trazSoAsFaltasDaMaisRecenteParaAMaisAntiga() = runBlocking {
        val repositorio = repositorioNoInstante(inicio)
        val ontem: LocalDate = hoje.minusDays(1)
        val anteontem: LocalDate = hoje.minusDays(2)
        repositorio.salvar("turma", anteontem, listOf(brunoFaltou))
        repositorio.salvar("turma", ontem, listOf(brunoFaltou.copy(presente = true, observacao = null)))
        repositorio.salvar("turma", hoje, listOf(brunoFaltou.copy(observacao = null)))

        val faltas: List<FaltaDoAluno> = repositorio.observarFaltasDoAluno("bruno").first()

        assertEquals(listOf(FaltaDoAluno(hoje, null), FaltaDoAluno(anteontem, "atestado")), faltas)
    }
}
