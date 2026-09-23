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
    val todasAsLinhas = MutableStateFlow<List<Turma>>(emptyList())

    fun buscarLinha(id: String): Turma {
        return todasAsLinhas.value.first { turma -> turma.id == id }
    }

    override fun observarTodas(): Flow<List<Turma>> {
        return todasAsLinhas.map { turmas -> turmas.filter { turma -> turma.deletedAt == null } }
    }

    override fun observarPorId(id: String): Flow<Turma?> {
        return todasAsLinhas.map { turmas ->
            turmas.firstOrNull { turma -> turma.id == id && turma.deletedAt == null }
        }
    }

    override suspend fun buscarPorId(id: String): Turma? {
        return observarPorId(id).first()
    }

    override suspend fun inserir(turma: Turma) {
        todasAsLinhas.value = todasAsLinhas.value + turma
    }

    override suspend fun atualizar(turma: Turma) {
        todasAsLinhas.value = todasAsLinhas.value.map { existente ->
            if (existente.id == turma.id) turma else existente
        }
    }

    override suspend fun marcarComoExcluida(id: String, agora: Instant) {
        val turmaExcluida = buscarLinha(id).copy(deletedAt = agora, updatedAt = agora)
        atualizar(turmaExcluida)
    }
}

class TurmaRepositoryImplTest {
    private val inicio = Instant.parse("2026-02-02T11:00:00Z")
    private val dao = FakeTurmaDao()

    private fun repositorioNoInstante(agora: Instant): TurmaRepositoryImpl {
        return TurmaRepositoryImpl(dao, Clock.fixed(agora, ZoneOffset.UTC))
    }

    @Test
    fun criar_geraUuidETimestamps() = runBlocking {
        val turma = repositorioNoInstante(inicio).criar("1º A", "1º ano", Periodo.MANHA, 2026)

        UUID.fromString(turma.id) // lança exceção se não for UUID válido
        assertEquals(inicio, turma.createdAt)
        assertEquals(inicio, turma.updatedAt)
        assertNull(turma.deletedAt)
    }

    @Test
    fun atualizar_mudaSoUpdatedAt() = runBlocking {
        val turma = repositorioNoInstante(inicio).criar("1º A", "1º ano", Periodo.MANHA, 2026)
        val umMinutoDepois = inicio.plusSeconds(60)

        repositorioNoInstante(umMinutoDepois).atualizar(turma.copy(nome = "1º B"))

        val salva = dao.buscarLinha(turma.id)
        assertEquals("1º B", salva.nome)
        assertEquals(inicio, salva.createdAt)
        assertEquals(umMinutoDepois, salva.updatedAt)
    }

    @Test
    fun excluir_fazSoftDelete() = runBlocking {
        val repositorio = repositorioNoInstante(inicio)
        val turma = repositorio.criar("1º A", "1º ano", Periodo.MANHA, 2026)

        repositorio.excluir(turma.id)

        assertTrue(repositorio.observarTurmas().first().isEmpty())
        assertNull(repositorio.buscarTurma(turma.id))
        assertEquals(inicio, dao.buscarLinha(turma.id).deletedAt)
    }
}
