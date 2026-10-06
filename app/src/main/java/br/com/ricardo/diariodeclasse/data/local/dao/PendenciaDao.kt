package br.com.ricardo.diariodeclasse.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import br.com.ricardo.diariodeclasse.data.local.entity.Pendencia
import br.com.ricardo.diariodeclasse.data.local.entity.StatusPendencia
import kotlinx.coroutines.flow.Flow

/**
 * A pendência não guarda a turma: ela é descoberta pelo aluno (JOIN com `alunos`).
 * Pendências de alunos excluídos deixam de aparecer junto com eles.
 */
@Dao
interface PendenciaDao {
    @Query(
        "SELECT pendencias.* FROM pendencias " +
            "INNER JOIN alunos ON alunos.id = pendencias.alunoId " +
            "WHERE alunos.turmaId = :turmaId AND pendencias.status = :status " +
            "AND alunos.deletedAt IS NULL AND pendencias.deletedAt IS NULL " +
            "ORDER BY pendencias.dataLembrete, pendencias.createdAt"
    )
    fun observarDaTurmaPorStatus(turmaId: String, status: StatusPendencia): Flow<List<Pendencia>>

    @Query("SELECT * FROM pendencias WHERE id = :id AND deletedAt IS NULL")
    suspend fun buscarPorId(id: String): Pendencia?

    /** Dentre os registros de falta informados, quais já têm alguma pendência ligada. */
    @Query(
        "SELECT DISTINCT registroPresencaId FROM pendencias " +
            "WHERE registroPresencaId IN (:registroPresencaIds) AND deletedAt IS NULL"
    )
    suspend fun buscarRegistrosComPendencia(registroPresencaIds: List<String>): List<String>

    @Insert
    suspend fun inserir(pendencia: Pendencia)

    @Update
    suspend fun atualizar(pendencia: Pendencia)
}
