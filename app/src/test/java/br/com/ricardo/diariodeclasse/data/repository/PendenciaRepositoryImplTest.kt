package br.com.ricardo.diariodeclasse.data.repository

import br.com.ricardo.diariodeclasse.data.local.dao.PendenciaDao
import br.com.ricardo.diariodeclasse.data.local.entity.Pendencia
import br.com.ricardo.diariodeclasse.data.local.entity.StatusPendencia
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

/** DAO em memória. Ignora a turma (o JOIN com alunos é testado no banco real). */
private class FakePendenciaDao : PendenciaDao {
    val linhas = MutableStateFlow<List<Pendencia>>(emptyList())

    override fun observarDaTurmaPorStatus(turmaId: String, status: StatusPendencia): Flow<List<Pendencia>> {
        return linhas.map { pendencias ->
            pendencias.filter { pendencia -> pendencia.status == status && pendencia.deletedAt == null }
        }
    }

    override suspend fun buscarPorId(id: String): Pendencia? {
        return linhas.value.firstOrNull { pendencia -> pendencia.id == id }
    }

    override suspend fun inserir(pendencia: Pendencia) {
        linhas.value = linhas.value + pendencia
    }

    override suspend fun atualizar(pendencia: Pendencia) {
        linhas.value = linhas.value.map { existente ->
            if (existente.id == pendencia.id) pendencia else existente
        }
    }
}

class PendenciaRepositoryImplTest {
    private val inicio = Instant.parse("2026-10-06T11:00:00Z")
    private val hoje = LocalDate.of(2026, 10, 6)
    private val dao = FakePendenciaDao()

    private fun repositorioNoInstante(agora: Instant): PendenciaRepositoryImpl {
        return PendenciaRepositoryImpl(dao, Clock.fixed(agora, ZoneOffset.UTC))
    }

    @Test
    fun criar_comecaPendenteESemVinculoComFalta() = runBlocking {
        val pendencia = repositorioNoInstante(inicio).criar("ana", "Ficha de matemática", hoje)

        assertEquals(StatusPendencia.PENDENTE, pendencia.status)
        assertNull(pendencia.registroPresencaId)
        assertNull(pendencia.entregueEm)
        assertEquals(inicio, pendencia.createdAt)
    }

    @Test
    fun marcarComoEntregue_saiDaListaDePendentesSemApagar() = runBlocking {
        val repositorio = repositorioNoInstante(inicio)
        val pendencia = repositorio.criar("ana", "Ficha de matemática", hoje)
        val umaHoraDepois = inicio.plusSeconds(3600)

        repositorioNoInstante(umaHoraDepois).marcarComoEntregue(pendencia.id)

        assertTrue(repositorio.observarPendentesDaTurma("turma").first().isEmpty())
        val salva = dao.buscarPorId(pendencia.id)!!
        assertEquals(StatusPendencia.ENTREGUE, salva.status)
        assertEquals(umaHoraDepois, salva.entregueEm)
        assertEquals(umaHoraDepois, salva.updatedAt)
    }

    @Test
    fun desfazerEntrega_voltaParaPendente() = runBlocking {
        val repositorio = repositorioNoInstante(inicio)
        val pendencia = repositorio.criar("ana", "Ficha de matemática", hoje)
        repositorio.marcarComoEntregue(pendencia.id)

        repositorio.desfazerEntrega(pendencia.id)

        val salva = dao.buscarPorId(pendencia.id)!!
        assertEquals(StatusPendencia.PENDENTE, salva.status)
        assertNull(salva.entregueEm)
        assertEquals(1, repositorio.observarPendentesDaTurma("turma").first().size)
    }

    @Test
    fun estaPendenteEm_consideraDataDoLembreteEStatus() = runBlocking {
        val repositorio = repositorioNoInstante(inicio)
        val paraOntem = repositorio.criar("ana", "A", hoje.minusDays(1))
        val paraHoje = repositorio.criar("ana", "B", hoje)
        val paraAmanha = repositorio.criar("ana", "C", hoje.plusDays(1))
        val entregue = paraHoje.copy(status = StatusPendencia.ENTREGUE)

        assertTrue(paraOntem.estaPendenteEm(hoje))
        assertTrue(paraHoje.estaPendenteEm(hoje))
        assertFalse(paraAmanha.estaPendenteEm(hoje))
        assertFalse(entregue.estaPendenteEm(hoje))
    }
}
