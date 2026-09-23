package br.com.ricardo.diariodeclasse.data.repository

import br.com.ricardo.diariodeclasse.data.local.dao.AlunoDao
import br.com.ricardo.diariodeclasse.data.local.entity.Aluno
import kotlinx.coroutines.flow.Flow
import java.time.Clock
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

interface AlunoRepository {
    fun observarAlunosDaTurma(turmaId: String): Flow<List<Aluno>>
    suspend fun criar(turmaId: String, nome: String): Aluno
    suspend fun renomear(aluno: Aluno, novoNome: String)
    suspend fun excluir(id: String)
}

class AlunoRepositoryImpl @Inject constructor(
    private val dao: AlunoDao,
    private val clock: Clock,
) : AlunoRepository {

    override fun observarAlunosDaTurma(turmaId: String): Flow<List<Aluno>> =
        dao.observarDaTurma(turmaId)

    override suspend fun criar(turmaId: String, nome: String): Aluno {
        val agora = Instant.now(clock)
        val aluno = Aluno(
            id = UUID.randomUUID().toString(),
            turmaId = turmaId,
            nome = nome,
            createdAt = agora,
            updatedAt = agora,
        )
        dao.inserir(aluno)
        return aluno
    }

    override suspend fun renomear(aluno: Aluno, novoNome: String) {
        val alunoAtualizado = aluno.copy(nome = novoNome, updatedAt = Instant.now(clock))
        dao.atualizar(alunoAtualizado)
    }

    override suspend fun excluir(id: String) {
        dao.marcarComoExcluido(id, Instant.now(clock))
    }
}
