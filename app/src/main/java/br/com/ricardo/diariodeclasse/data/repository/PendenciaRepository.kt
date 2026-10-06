package br.com.ricardo.diariodeclasse.data.repository

import br.com.ricardo.diariodeclasse.data.local.dao.PendenciaDao
import br.com.ricardo.diariodeclasse.data.local.entity.Pendencia
import br.com.ricardo.diariodeclasse.data.local.entity.StatusPendencia
import kotlinx.coroutines.flow.Flow
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

interface PendenciaRepository {
    /** Só as que ainda não foram entregues, das mais antigas para as mais novas. */
    fun observarPendentesDaTurma(turmaId: String): Flow<List<Pendencia>>

    suspend fun criar(
        alunoId: String,
        descricao: String,
        dataLembrete: LocalDate,
        registroPresencaId: String? = null,
    ): Pendencia

    suspend fun marcarComoEntregue(id: String)

    /** Volta a pendência para "pendente" (botão "Desfazer" depois de marcar por engano). */
    suspend fun desfazerEntrega(id: String)
}

class PendenciaRepositoryImpl @Inject constructor(
    private val dao: PendenciaDao,
    private val clock: Clock,
) : PendenciaRepository {

    override fun observarPendentesDaTurma(turmaId: String): Flow<List<Pendencia>> {
        return dao.observarDaTurmaPorStatus(turmaId, StatusPendencia.PENDENTE)
    }

    override suspend fun criar(
        alunoId: String,
        descricao: String,
        dataLembrete: LocalDate,
        registroPresencaId: String?,
    ): Pendencia {
        val agora: Instant = Instant.now(clock)
        val pendencia = Pendencia(
            id = UUID.randomUUID().toString(),
            alunoId = alunoId,
            descricao = descricao,
            dataLembrete = dataLembrete,
            status = StatusPendencia.PENDENTE,
            registroPresencaId = registroPresencaId,
            createdAt = agora,
            updatedAt = agora,
        )
        dao.inserir(pendencia)
        return pendencia
    }

    override suspend fun marcarComoEntregue(id: String) {
        val pendencia: Pendencia = dao.buscarPorId(id) ?: return
        val agora: Instant = Instant.now(clock)
        val entregue = pendencia.copy(
            status = StatusPendencia.ENTREGUE,
            entregueEm = agora,
            updatedAt = agora,
        )
        dao.atualizar(entregue)
    }

    override suspend fun desfazerEntrega(id: String) {
        val pendencia: Pendencia = dao.buscarPorId(id) ?: return
        val pendenteDeNovo = pendencia.copy(
            status = StatusPendencia.PENDENTE,
            entregueEm = null,
            updatedAt = Instant.now(clock),
        )
        dao.atualizar(pendenteDeNovo)
    }
}
