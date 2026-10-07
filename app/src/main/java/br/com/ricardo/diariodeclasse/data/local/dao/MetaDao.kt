package br.com.ricardo.diariodeclasse.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import br.com.ricardo.diariodeclasse.data.local.entity.AlunoNaMeta
import br.com.ricardo.diariodeclasse.data.local.entity.Meta
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * As consultas de metas também olham a métrica: excluir a métrica faz as metas
 * dela sumirem, como excluir a turma faz os alunos sumirem.
 */
@Dao
interface MetaDao {
    /** Em andamento primeiro, pelo prazo mais próximo; as encerradas por último. */
    @Query(
        "SELECT metas.* FROM metas " +
            "INNER JOIN metricas ON metricas.id = metas.metricaId " +
            "WHERE metas.turmaId = :turmaId AND metas.deletedAt IS NULL AND metricas.deletedAt IS NULL " +
            "ORDER BY metas.encerradaEm IS NOT NULL, metas.prazo"
    )
    fun observarDaTurma(turmaId: String): Flow<List<Meta>>

    @Query(
        "SELECT metas.* FROM metas " +
            "INNER JOIN metricas ON metricas.id = metas.metricaId " +
            "WHERE metas.id = :id AND metas.deletedAt IS NULL AND metricas.deletedAt IS NULL"
    )
    fun observarPorId(id: String): Flow<Meta?>

    @Query("SELECT * FROM metas WHERE id = :id AND deletedAt IS NULL")
    suspend fun buscarPorId(id: String): Meta?

    @Query("SELECT * FROM alunos_na_meta WHERE metaId = :metaId AND deletedAt IS NULL")
    fun observarAlunosDaMeta(metaId: String): Flow<List<AlunoNaMeta>>

    @Query(
        "SELECT alunos_na_meta.* FROM alunos_na_meta " +
            "INNER JOIN metas ON metas.id = alunos_na_meta.metaId " +
            "WHERE metas.turmaId = :turmaId AND metas.deletedAt IS NULL AND alunos_na_meta.deletedAt IS NULL"
    )
    fun observarAlunosDasMetasDaTurma(turmaId: String): Flow<List<AlunoNaMeta>>

    /**
     * Inclui os removidos: o índice único (meta + aluno) não deixa criar outra
     * linha, então um aluno que volta para a meta reaproveita a antiga.
     */
    @Query("SELECT * FROM alunos_na_meta WHERE metaId = :metaId")
    suspend fun buscarTodosOsAlunosDaMeta(metaId: String): List<AlunoNaMeta>

    @Upsert
    suspend fun salvarMeta(meta: Meta)

    @Upsert
    suspend fun salvarAlunos(alunos: List<AlunoNaMeta>)

    @Transaction
    suspend fun salvarMetaComAlunos(meta: Meta, alunos: List<AlunoNaMeta>) {
        salvarMeta(meta)
        salvarAlunos(alunos)
    }

    @Query("UPDATE metas SET deletedAt = :agora, updatedAt = :agora WHERE id = :id")
    suspend fun marcarComoExcluida(id: String, agora: Instant)
}
