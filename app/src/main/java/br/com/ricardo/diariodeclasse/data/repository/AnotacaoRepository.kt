package br.com.ricardo.diariodeclasse.data.repository

import br.com.ricardo.diariodeclasse.data.local.dao.AnotacaoDao
import br.com.ricardo.diariodeclasse.data.local.entity.Anotacao
import kotlinx.coroutines.flow.Flow
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

interface AnotacaoRepository {
    fun observarDoAluno(alunoId: String): Flow<List<Anotacao>>
    suspend fun criar(alunoId: String, texto: String, data: LocalDate): Anotacao
    suspend fun editarTexto(id: String, texto: String)
    suspend fun excluir(id: String)

    /** Desfaz uma exclusão (botão "Desfazer" do aviso). */
    suspend fun restaurar(id: String)
}

class AnotacaoRepositoryImpl @Inject constructor(
    private val dao: AnotacaoDao,
    private val clock: Clock,
) : AnotacaoRepository {

    override fun observarDoAluno(alunoId: String): Flow<List<Anotacao>> {
        return dao.observarDoAluno(alunoId)
    }

    override suspend fun criar(alunoId: String, texto: String, data: LocalDate): Anotacao {
        val agora: Instant = Instant.now(clock)
        val anotacao = Anotacao(
            id = UUID.randomUUID().toString(),
            alunoId = alunoId,
            texto = texto,
            data = data,
            createdAt = agora,
            updatedAt = agora,
        )
        dao.inserir(anotacao)
        return anotacao
    }

    override suspend fun editarTexto(id: String, texto: String) {
        val anotacao: Anotacao = dao.buscarPorId(id) ?: return
        val editada = anotacao.copy(texto = texto, updatedAt = Instant.now(clock))
        dao.atualizar(editada)
    }

    override suspend fun excluir(id: String) {
        dao.marcarComoExcluida(id, Instant.now(clock))
    }

    override suspend fun restaurar(id: String) {
        val anotacao: Anotacao = dao.buscarPorId(id) ?: return
        val restaurada = anotacao.copy(deletedAt = null, updatedAt = Instant.now(clock))
        dao.atualizar(restaurada)
    }
}
