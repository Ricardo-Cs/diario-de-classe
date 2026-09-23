package br.com.ricardo.diariodeclasse.data.repository

import br.com.ricardo.diariodeclasse.data.local.dao.TurmaDao
import br.com.ricardo.diariodeclasse.data.local.entity.Periodo
import br.com.ricardo.diariodeclasse.data.local.entity.Turma
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
import java.time.ZoneOffset
import java.util.UUID

/** DAO em memória que imita o filtro de soft delete das queries reais. */
private class FakeTurmaDao : TurmaDao {
    val linhas = MutableStateFlow<Map<String, Turma>>(emptyMap())

    override fun observarTodas(): Flow<List<Turma>> =
        linhas.map { it.values.filter { t -> t.deletedAt == null } }

    override fun observarPorId(id: String): Flow<Turma?> =
        linhas.map { it[id]?.takeIf { t -> t.deletedAt == null } }

    override suspend fun inserir(turma: Turma) {
        linhas.value += turma.id to turma
    }

    override suspend fun atualizar(turma: Turma) {
        linhas.value += turma.id to turma
    }

    override suspend fun marcarComoExcluida(id: String, agora: Instant) {
        val turma = linhas.value.getValue(id)
        linhas.value += id to turma.copy(deletedAt = agora, updatedAt = agora)
    }
}

class TurmaRepositoryImplTest {
    private val inicio = Instant.parse("2026-02-02T11:00:00Z")
    private val dao = FakeTurmaDao()

    private fun repositorio(agora: Instant) =
        TurmaRepositoryImpl(dao, Clock.fixed(agora, ZoneOffset.UTC))

    @Test
    fun criar_geraUuidETimestamps() = runBlocking {
        val turma = repositorio(inicio).criar("1º A", "1º ano", Periodo.MANHA, 2026)

        UUID.fromString(turma.id) // lança exceção se não for UUID válido
        assertEquals(inicio, turma.createdAt)
        assertEquals(inicio, turma.updatedAt)
        assertNull(turma.deletedAt)
    }

    @Test
    fun atualizar_mudaSoUpdatedAt() = runBlocking {
        val turma = repositorio(inicio).criar("1º A", "1º ano", Periodo.MANHA, 2026)
        val depois = inicio.plusSeconds(60)

        repositorio(depois).atualizar(turma.copy(nome = "1º B"))

        val salva = dao.linhas.value.getValue(turma.id)
        assertEquals("1º B", salva.nome)
        assertEquals(inicio, salva.createdAt)
        assertEquals(depois, salva.updatedAt)
    }

    @Test
    fun excluir_fazSoftDelete() = runBlocking {
        val repo = repositorio(inicio)
        val turma = repo.criar("1º A", "1º ano", Periodo.MANHA, 2026)

        repo.excluir(turma.id)

        assertTrue(repo.observarTurmas().first().isEmpty())
        assertEquals(inicio, dao.linhas.value.getValue(turma.id).deletedAt)
    }
}
