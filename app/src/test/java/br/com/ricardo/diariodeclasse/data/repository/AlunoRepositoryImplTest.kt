package br.com.ricardo.diariodeclasse.data.repository

import br.com.ricardo.diariodeclasse.data.local.dao.AlunoDao
import br.com.ricardo.diariodeclasse.data.local.entity.Aluno
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/** DAO em memória que imita o filtro por turma e o soft delete das queries reais. */
private class FakeAlunoDao : AlunoDao {
    val todasAsLinhas = MutableStateFlow<List<Aluno>>(emptyList())

    fun buscarLinha(id: String): Aluno {
        return todasAsLinhas.value.first { aluno -> aluno.id == id }
    }

    override fun observarDaTurma(turmaId: String): Flow<List<Aluno>> {
        return todasAsLinhas.map { alunos ->
            alunos.filter { aluno -> aluno.turmaId == turmaId && aluno.deletedAt == null }
        }
    }

    override fun observarPorId(id: String): Flow<Aluno?> {
        return todasAsLinhas.map { alunos ->
            alunos.firstOrNull { aluno -> aluno.id == id && aluno.deletedAt == null }
        }
    }

    override suspend fun inserir(aluno: Aluno) {
        todasAsLinhas.value = todasAsLinhas.value + aluno
    }

    override suspend fun atualizar(aluno: Aluno) {
        todasAsLinhas.value = todasAsLinhas.value.map { existente ->
            if (existente.id == aluno.id) aluno else existente
        }
    }

    override suspend fun marcarComoExcluido(id: String, agora: Instant) {
        val alunoExcluido = buscarLinha(id).copy(deletedAt = agora, updatedAt = agora)
        atualizar(alunoExcluido)
    }
}

class AlunoRepositoryImplTest {
    private val inicio = Instant.parse("2026-02-02T11:00:00Z")
    private val dao = FakeAlunoDao()

    private fun repositorioNoInstante(agora: Instant): AlunoRepositoryImpl {
        return AlunoRepositoryImpl(dao, Clock.fixed(agora, ZoneOffset.UTC))
    }

    @Test
    fun criar_vinculaATurmaEPreencheTimestamps() = runBlocking {
        val aluno = repositorioNoInstante(inicio).criar(turmaId = "turma-1", nome = "Ana")

        assertEquals("turma-1", aluno.turmaId)
        assertEquals("Ana", aluno.nome)
        assertEquals(inicio, aluno.createdAt)
        assertEquals(inicio, aluno.updatedAt)
    }

    @Test
    fun observarAlunosDaTurma_trazSoAlunosDaquelaTurma() = runBlocking {
        val repositorio = repositorioNoInstante(inicio)
        repositorio.criar(turmaId = "turma-1", nome = "Ana")
        repositorio.criar(turmaId = "turma-2", nome = "Bruno")

        val alunosDaTurma1 = repositorio.observarAlunosDaTurma("turma-1").first()

        assertEquals(listOf("Ana"), alunosDaTurma1.map { aluno -> aluno.nome })
    }

    @Test
    fun renomear_mudaNomeEUpdatedAt() = runBlocking {
        val aluno = repositorioNoInstante(inicio).criar(turmaId = "turma-1", nome = "Ana")
        val umMinutoDepois = inicio.plusSeconds(60)

        repositorioNoInstante(umMinutoDepois).renomear(aluno, "Ana Clara")

        val salvo = dao.buscarLinha(aluno.id)
        assertEquals("Ana Clara", salvo.nome)
        assertEquals(inicio, salvo.createdAt)
        assertEquals(umMinutoDepois, salvo.updatedAt)
    }

    @Test
    fun excluir_escondeAlunoDaTurma() = runBlocking {
        val repositorio = repositorioNoInstante(inicio)
        val aluno = repositorio.criar(turmaId = "turma-1", nome = "Ana")

        repositorio.excluir(aluno.id)

        assertEquals(emptyList<Aluno>(), repositorio.observarAlunosDaTurma("turma-1").first())
        assertEquals(inicio, dao.buscarLinha(aluno.id).deletedAt)
    }
}
