package br.com.ricardo.diariodeclasse.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import br.com.ricardo.diariodeclasse.data.local.entity.Pendencia
import br.com.ricardo.diariodeclasse.data.local.entity.PendenciaComOrigem
import br.com.ricardo.diariodeclasse.data.local.entity.StatusPendencia
import kotlinx.coroutines.flow.Flow

/**
 * A pendência não guarda a turma: ela é descoberta pelo aluno (JOIN com `alunos`).
 * Pendências de alunos excluídos deixam de aparecer junto com eles.
 */
@Dao
interface PendenciaDao {
    /**
     * `LEFT JOIN` porque a pendência avulsa não tem falta: nesse caso as colunas
     * da chamada vêm nulas e `dataDaFalta` fica `null`.
     */
    @Query(
        "SELECT pendencias.*, chamadas.data AS dataDaFalta FROM pendencias " +
            "INNER JOIN alunos ON alunos.id = pendencias.alunoId " +
            "LEFT JOIN registros_presenca ON registros_presenca.id = pendencias.registroPresencaId " +
            "LEFT JOIN chamadas ON chamadas.id = registros_presenca.chamadaId " +
            "WHERE alunos.turmaId = :turmaId AND pendencias.status = :status " +
            "AND alunos.deletedAt IS NULL AND pendencias.deletedAt IS NULL " +
            "ORDER BY pendencias.dataLembrete, pendencias.createdAt"
    )
    fun observarDaTurmaPorStatus(turmaId: String, status: StatusPendencia): Flow<List<PendenciaComOrigem>>

    /** Mesma consulta acima, filtrando por um aluno em vez da turma inteira. */
    @Query(
        "SELECT pendencias.*, chamadas.data AS dataDaFalta FROM pendencias " +
            "LEFT JOIN registros_presenca ON registros_presenca.id = pendencias.registroPresencaId " +
            "LEFT JOIN chamadas ON chamadas.id = registros_presenca.chamadaId " +
            "WHERE pendencias.alunoId = :alunoId AND pendencias.status = :status " +
            "AND pendencias.deletedAt IS NULL " +
            "ORDER BY pendencias.dataLembrete, pendencias.createdAt"
    )
    fun observarDoAlunoPorStatus(alunoId: String, status: StatusPendencia): Flow<List<PendenciaComOrigem>>

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
