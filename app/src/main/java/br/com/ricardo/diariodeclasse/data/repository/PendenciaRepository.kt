package br.com.ricardo.diariodeclasse.data.repository

import br.com.ricardo.diariodeclasse.data.local.dao.PendenciaDao
import br.com.ricardo.diariodeclasse.data.local.entity.Pendencia
import br.com.ricardo.diariodeclasse.data.local.entity.PendenciaComOrigem
import br.com.ricardo.diariodeclasse.data.local.entity.PendenciaParaLembrete
import br.com.ricardo.diariodeclasse.data.local.entity.StatusPendencia
import kotlinx.coroutines.flow.Flow
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

interface PendenciaRepository {
    /** Só as que ainda não foram entregues, das mais antigas para as mais novas. */
    fun observarPendentesDaTurma(turmaId: String): Flow<List<PendenciaComOrigem>>

    /** Só as não entregues de um aluno, na mesma ordem da turma. */
    fun observarPendentesDoAluno(alunoId: String): Flow<List<PendenciaComOrigem>>

    /** Não entregues, de todas as turmas, com lembrete para [data] ou já vencido. */
    suspend fun buscarParaLembrete(data: LocalDate): List<PendenciaParaLembrete>

    suspend fun criar(
        alunoId: String,
        descricao: String,
        dataLembrete: LocalDate,
        registroPresencaId: String? = null,
    ): Pendencia

    /** Altera a atividade e/ou a data do lembrete. O aluno não muda. */
    suspend fun editar(id: String, descricao: String, dataLembrete: LocalDate)

    /** Dentre os registros de falta informados, devolve os que já têm pendência ligada. */
    suspend fun buscarFaltasComPendencia(registroPresencaIds: List<String>): List<String>

    suspend fun marcarComoEntregue(id: String)

    /** Volta a pendência para "pendente" (botão "Desfazer" depois de marcar por engano). */
    suspend fun desfazerEntrega(id: String)
}

class PendenciaRepositoryImpl @Inject constructor(
    private val dao: PendenciaDao,
    private val clock: Clock,
) : PendenciaRepository {

    override fun observarPendentesDaTurma(turmaId: String): Flow<List<PendenciaComOrigem>> {
        return dao.observarDaTurmaPorStatus(turmaId, StatusPendencia.PENDENTE)
    }

    override fun observarPendentesDoAluno(alunoId: String): Flow<List<PendenciaComOrigem>> {
        return dao.observarDoAlunoPorStatus(alunoId, StatusPendencia.PENDENTE)
    }

    override suspend fun buscarParaLembrete(data: LocalDate): List<PendenciaParaLembrete> {
        return dao.buscarParaLembrete(data, StatusPendencia.PENDENTE)
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

    override suspend fun editar(id: String, descricao: String, dataLembrete: LocalDate) {
        val pendencia: Pendencia = dao.buscarPorId(id) ?: return
        val editada = pendencia.copy(
            descricao = descricao,
            dataLembrete = dataLembrete,
            updatedAt = Instant.now(clock),
        )
        dao.atualizar(editada)
    }

    override suspend fun buscarFaltasComPendencia(registroPresencaIds: List<String>): List<String> {
        if (registroPresencaIds.isEmpty()) {
            return emptyList()
        }
        return dao.buscarRegistrosComPendencia(registroPresencaIds)
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
