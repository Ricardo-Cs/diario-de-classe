package br.com.ricardo.diariodeclasse.data.repository

import br.com.ricardo.diariodeclasse.data.local.dao.AnotacaoDao
import br.com.ricardo.diariodeclasse.data.local.entity.Anotacao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

/** DAO em memória que imita o filtro de soft delete e a ordem (mais recentes primeiro). */
private class FakeAnotacaoDao : AnotacaoDao {
    val linhas = MutableStateFlow<List<Anotacao>>(emptyList())

    override fun observarDoAluno(alunoId: String): Flow<List<Anotacao>> {
        return linhas.map { anotacoes ->
            val doAluno = anotacoes.filter { anotacao -> anotacao.alunoId == alunoId && anotacao.deletedAt == null }
            doAluno.sortedByDescending { anotacao -> anotacao.data }
        }
    }

    override suspend fun buscarPorId(id: String): Anotacao? {
        return linhas.value.firstOrNull { anotacao -> anotacao.id == id }
    }

    override suspend fun inserir(anotacao: Anotacao) {
        linhas.value = linhas.value + anotacao
    }

    override suspend fun atualizar(anotacao: Anotacao) {
        linhas.value = linhas.value.map { existente ->
            if (existente.id == anotacao.id) anotacao else existente
        }
    }

    override suspend fun marcarComoExcluida(id: String, agora: Instant) {
        val excluida = buscarPorId(id)!!.copy(deletedAt = agora, updatedAt = agora)
        atualizar(excluida)
    }
}

class AnotacaoRepositoryImplTest {
    private val inicio = Instant.parse("2026-10-06T11:00:00Z")
    private val hoje = LocalDate.of(2026, 10, 6)
    private val dao = FakeAnotacaoDao()

    private fun repositorioNoInstante(agora: Instant): AnotacaoRepositoryImpl {
        return AnotacaoRepositoryImpl(dao, Clock.fixed(agora, ZoneOffset.UTC))
    }

    @Test
    fun criar_geraUuidETimestamps() = runBlocking {
        val anotacao = repositorioNoInstante(inicio).criar("ana", "Leu sozinha hoje", hoje)

        UUID.fromString(anotacao.id)
        assertEquals(hoje, anotacao.data)
        assertEquals(inicio, anotacao.createdAt)
        assertEquals(inicio, anotacao.updatedAt)
    }

    @Test
    fun observarDoAluno_maisRecentesPrimeiro() = runBlocking {
        val repositorio = repositorioNoInstante(inicio)
        repositorio.criar("ana", "Antiga", hoje.minusDays(3))
        repositorio.criar("ana", "Nova", hoje)
        repositorio.criar("bruno", "De outro aluno", hoje)

        val anotacoes = repositorio.observarDoAluno("ana").first()

        assertEquals(listOf("Nova", "Antiga"), anotacoes.map { anotacao -> anotacao.texto })
    }

    @Test
    fun editarTexto_mantemDataEAtualizaUpdatedAt() = runBlocking {
        val anotacao = repositorioNoInstante(inicio).criar("ana", "Leu sozinha", hoje)
        val umaHoraDepois = inicio.plusSeconds(3600)

        repositorioNoInstante(umaHoraDepois).editarTexto(anotacao.id, "Leu sozinha a página toda")

        val salva = dao.buscarPorId(anotacao.id)!!
        assertEquals("Leu sozinha a página toda", salva.texto)
        assertEquals(hoje, salva.data)
        assertEquals(inicio, salva.createdAt)
        assertEquals(umaHoraDepois, salva.updatedAt)
    }

    @Test
    fun excluirERestaurar() = runBlocking {
        val repositorio = repositorioNoInstante(inicio)
        val anotacao = repositorio.criar("ana", "Leu sozinha", hoje)

        repositorio.excluir(anotacao.id)
        assertTrue(repositorio.observarDoAluno("ana").first().isEmpty())
        assertEquals(inicio, dao.buscarPorId(anotacao.id)!!.deletedAt)

        repositorio.restaurar(anotacao.id)
        assertEquals(1, repositorio.observarDoAluno("ana").first().size)
        assertNull(dao.buscarPorId(anotacao.id)!!.deletedAt)
    }
}
