package br.com.ricardo.diariodeclasse.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import br.com.ricardo.diariodeclasse.data.local.entity.Metrica
import br.com.ricardo.diariodeclasse.data.local.entity.NivelDaMetrica
import br.com.ricardo.diariodeclasse.data.local.entity.NivelRegistradoDoAluno
import br.com.ricardo.diariodeclasse.data.local.entity.ResultadoDaSondagem
import br.com.ricardo.diariodeclasse.data.local.entity.ResultadoDatado
import br.com.ricardo.diariodeclasse.data.local.entity.Sondagem
import br.com.ricardo.diariodeclasse.data.local.entity.SondagemResumida
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

/** Métricas, os níveis de cada uma e as sondagens com seus resultados. */
@Dao
interface MetricaDao {
    @Query(
        "SELECT * FROM metricas WHERE turmaId = :turmaId AND deletedAt IS NULL " +
            "ORDER BY nome COLLATE LOCALIZED"
    )
    fun observarDaTurma(turmaId: String): Flow<List<Metrica>>

    @Query("SELECT * FROM metricas WHERE id = :id AND deletedAt IS NULL")
    fun observarPorId(id: String): Flow<Metrica?>

    @Query("SELECT * FROM metricas WHERE id = :id AND deletedAt IS NULL")
    suspend fun buscarPorId(id: String): Metrica?

    @Query("SELECT * FROM niveis_da_metrica WHERE metricaId = :metricaId AND deletedAt IS NULL ORDER BY ordem")
    fun observarNiveis(metricaId: String): Flow<List<NivelDaMetrica>>

    /** Inclui os removidos: ao salvar a métrica, o repositório compara com a lista nova. */
    @Query("SELECT * FROM niveis_da_metrica WHERE metricaId = :metricaId")
    suspend fun buscarTodosOsNiveis(metricaId: String): List<NivelDaMetrica>

    /** Níveis de todas as métricas da turma, para a aba Acompanhamento montar os resumos de uma vez. */
    @Query(
        "SELECT niveis_da_metrica.* FROM niveis_da_metrica " +
            "INNER JOIN metricas ON metricas.id = niveis_da_metrica.metricaId " +
            "WHERE metricas.turmaId = :turmaId " +
            "AND metricas.deletedAt IS NULL AND niveis_da_metrica.deletedAt IS NULL " +
            "ORDER BY niveis_da_metrica.ordem"
    )
    fun observarNiveisDaTurma(turmaId: String): Flow<List<NivelDaMetrica>>

    /**
     * Níveis que não podem ser removidos da escala: têm alunos avaliados neles ou
     * são usados por uma meta. `UNION` junta os ids das três consultas sem repetir.
     */
    @Query(
        "SELECT resultados_da_sondagem.nivelId FROM resultados_da_sondagem " +
            "INNER JOIN sondagens ON sondagens.id = resultados_da_sondagem.sondagemId " +
            "WHERE sondagens.metricaId = :metricaId " +
            "AND sondagens.deletedAt IS NULL AND resultados_da_sondagem.deletedAt IS NULL " +
            "UNION " +
            "SELECT nivelAlvoId FROM metas WHERE metricaId = :metricaId AND nivelAlvoId IS NOT NULL " +
            "AND deletedAt IS NULL " +
            "UNION " +
            "SELECT alunos_na_meta.nivelInicialId FROM alunos_na_meta " +
            "INNER JOIN metas ON metas.id = alunos_na_meta.metaId " +
            "WHERE metas.metricaId = :metricaId AND alunos_na_meta.nivelInicialId IS NOT NULL " +
            "AND metas.deletedAt IS NULL AND alunos_na_meta.deletedAt IS NULL"
    )
    suspend fun buscarIdsDeNiveisEmUso(metricaId: String): List<String>

    @Query(
        "SELECT sondagens.metricaId AS metricaId, resultados_da_sondagem.alunoId AS alunoId, " +
            "resultados_da_sondagem.nivelId AS nivelId, sondagens.data AS data " +
            "FROM resultados_da_sondagem " +
            "INNER JOIN sondagens ON sondagens.id = resultados_da_sondagem.sondagemId " +
            "INNER JOIN metricas ON metricas.id = sondagens.metricaId " +
            "WHERE metricas.turmaId = :turmaId AND metricas.deletedAt IS NULL " +
            "AND sondagens.deletedAt IS NULL AND resultados_da_sondagem.deletedAt IS NULL"
    )
    fun observarResultadosDaTurma(turmaId: String): Flow<List<ResultadoDatado>>

    @Query(
        "SELECT sondagens.metricaId AS metricaId, resultados_da_sondagem.alunoId AS alunoId, " +
            "resultados_da_sondagem.nivelId AS nivelId, sondagens.data AS data " +
            "FROM resultados_da_sondagem " +
            "INNER JOIN sondagens ON sondagens.id = resultados_da_sondagem.sondagemId " +
            "WHERE sondagens.metricaId = :metricaId " +
            "AND sondagens.deletedAt IS NULL AND resultados_da_sondagem.deletedAt IS NULL"
    )
    fun observarResultadosDaMetrica(metricaId: String): Flow<List<ResultadoDatado>>

    /** Histórico do aluno em todas as métricas, do mais recente para o mais antigo. */
    @Query(
        "SELECT metricas.id AS metricaId, metricas.nome AS nomeDaMetrica, " +
            "niveis_da_metrica.id AS nivelId, niveis_da_metrica.nome AS nomeDoNivel, sondagens.data AS data " +
            "FROM resultados_da_sondagem " +
            "INNER JOIN sondagens ON sondagens.id = resultados_da_sondagem.sondagemId " +
            "INNER JOIN metricas ON metricas.id = sondagens.metricaId " +
            "INNER JOIN niveis_da_metrica ON niveis_da_metrica.id = resultados_da_sondagem.nivelId " +
            "WHERE resultados_da_sondagem.alunoId = :alunoId AND metricas.deletedAt IS NULL " +
            "AND sondagens.deletedAt IS NULL AND resultados_da_sondagem.deletedAt IS NULL " +
            "ORDER BY metricas.nome COLLATE LOCALIZED, sondagens.data DESC"
    )
    fun observarNiveisDoAluno(alunoId: String): Flow<List<NivelRegistradoDoAluno>>

    /**
     * Sondagens da métrica com quantos alunos (ainda na turma) foram avaliados.
     * O `LEFT JOIN` mantém na lista a sondagem mesmo que ninguém conte.
     */
    @Query(
        "SELECT sondagens.id AS id, sondagens.data AS data, COUNT(alunos.id) AS quantidadeDeAlunos " +
            "FROM sondagens " +
            "LEFT JOIN resultados_da_sondagem ON resultados_da_sondagem.sondagemId = sondagens.id " +
            "AND resultados_da_sondagem.deletedAt IS NULL " +
            "LEFT JOIN alunos ON alunos.id = resultados_da_sondagem.alunoId AND alunos.deletedAt IS NULL " +
            "WHERE sondagens.metricaId = :metricaId AND sondagens.deletedAt IS NULL " +
            "GROUP BY sondagens.id ORDER BY sondagens.data DESC"
    )
    fun observarSondagens(metricaId: String): Flow<List<SondagemResumida>>

    /**
     * Inclui a excluída: o índice único (métrica + data) não deixa criar outra no
     * mesmo dia, então uma sondagem excluída é reaproveitada.
     */
    @Query("SELECT * FROM sondagens WHERE metricaId = :metricaId AND data = :data")
    suspend fun buscarSondagem(metricaId: String, data: LocalDate): Sondagem?

    /** Inclui os excluídos, pelo mesmo motivo de [buscarSondagem]. */
    @Query("SELECT * FROM resultados_da_sondagem WHERE sondagemId = :sondagemId")
    suspend fun buscarResultados(sondagemId: String): List<ResultadoDaSondagem>

    @Upsert
    suspend fun salvarMetrica(metrica: Metrica)

    @Upsert
    suspend fun salvarNiveis(niveis: List<NivelDaMetrica>)

    @Upsert
    suspend fun salvarSondagem(sondagem: Sondagem)

    @Upsert
    suspend fun salvarResultados(resultados: List<ResultadoDaSondagem>)

    @Transaction
    suspend fun salvarMetricaComNiveis(metrica: Metrica, niveis: List<NivelDaMetrica>) {
        salvarMetrica(metrica)
        salvarNiveis(niveis)
    }

    @Transaction
    suspend fun salvarSondagemComResultados(sondagem: Sondagem, resultados: List<ResultadoDaSondagem>) {
        salvarSondagem(sondagem)
        salvarResultados(resultados)
    }

    @Query("UPDATE metricas SET deletedAt = :agora, updatedAt = :agora WHERE id = :id")
    suspend fun marcarComoExcluida(id: String, agora: Instant)
}
