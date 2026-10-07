package br.com.ricardo.diariodeclasse.data.repository

import br.com.ricardo.diariodeclasse.data.local.dao.LembreteDao
import br.com.ricardo.diariodeclasse.data.local.entity.Lembrete
import kotlinx.coroutines.flow.Flow
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

/** Lembretes da professora (entregas, prazos dela). Não confundir com as pendências dos alunos. */
interface LembreteRepository {
    fun observarEmAberto(): Flow<List<Lembrete>>
    fun observarConcluidosRecentes(): Flow<List<Lembrete>>

    /** Em aberto com data até [data]: os do dia e os atrasados. */
    suspend fun buscarParaNotificar(data: LocalDate): List<Lembrete>

    suspend fun criar(descricao: String, data: LocalDate): Lembrete
    suspend fun editar(id: String, descricao: String, data: LocalDate)
    suspend fun marcarComoConcluido(id: String)

    /** Desfaz a conclusão (botão "Desfazer" do aviso, ou tocar de novo num concluído). */
    suspend fun reabrir(id: String)
    suspend fun excluir(id: String)

    /** Desfaz uma exclusão (botão "Desfazer" do aviso). */
    suspend fun restaurar(id: String)
}

class LembreteRepositoryImpl @Inject constructor(
    private val dao: LembreteDao,
    private val clock: Clock,
) : LembreteRepository {

    override fun observarEmAberto(): Flow<List<Lembrete>> {
        return dao.observarEmAberto()
    }

    override fun observarConcluidosRecentes(): Flow<List<Lembrete>> {
        return dao.observarConcluidos(LIMITE_DE_CONCLUIDOS)
    }

    override suspend fun buscarParaNotificar(data: LocalDate): List<Lembrete> {
        return dao.buscarAte(data)
    }

    override suspend fun criar(descricao: String, data: LocalDate): Lembrete {
        val agora: Instant = Instant.now(clock)
        val lembrete = Lembrete(
            id = UUID.randomUUID().toString(),
            descricao = descricao,
            data = data,
            createdAt = agora,
            updatedAt = agora,
        )
        dao.inserir(lembrete)
        return lembrete
    }

    override suspend fun editar(id: String, descricao: String, data: LocalDate) {
        val lembrete: Lembrete = dao.buscarPorId(id) ?: return
        dao.atualizar(lembrete.copy(descricao = descricao, data = data, updatedAt = Instant.now(clock)))
    }

    override suspend fun marcarComoConcluido(id: String) {
        val lembrete: Lembrete = dao.buscarPorId(id) ?: return
        val agora: Instant = Instant.now(clock)
        dao.atualizar(lembrete.copy(concluidoEm = agora, updatedAt = agora))
    }

    override suspend fun reabrir(id: String) {
        val lembrete: Lembrete = dao.buscarPorId(id) ?: return
        dao.atualizar(lembrete.copy(concluidoEm = null, updatedAt = Instant.now(clock)))
    }

    override suspend fun excluir(id: String) {
        dao.marcarComoExcluido(id, Instant.now(clock))
    }

    override suspend fun restaurar(id: String) {
        val lembrete: Lembrete = dao.buscarPorId(id) ?: return
        dao.atualizar(lembrete.copy(deletedAt = null, updatedAt = Instant.now(clock)))
    }

    companion object {
        /** Os concluídos servem só para conferir o que foi feito há pouco. */
        private const val LIMITE_DE_CONCLUIDOS = 20
    }
}
