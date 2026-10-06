package br.com.ricardo.diariodeclasse.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import br.com.ricardo.diariodeclasse.data.local.entity.StatusPendencia
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Uma linha da consulta de atividade recente. As três tabelas (chamadas, pendências,
 * anotações) são "encaixadas" nas mesmas colunas; as que não se aplicam vêm `null`.
 * [origem] diz de qual tabela a linha veio.
 */
data class LinhaDeAtividade(
    val origem: String,
    val id: String,
    /** Quando aconteceu a ação mais recente (para pendência entregue, o momento da entrega). */
    val momento: Instant,
    val criadoEm: Instant,
    val status: StatusPendencia?,
    val nomeDoAluno: String?,
    val texto: String?,
    val ausentes: Int?,
)

/**
 * Não existe tabela de "log": a atividade recente é montada a partir dos próprios
 * registros, pelo `updatedAt` (ou `entregueEm`). `UNION ALL` empilha os resultados
 * das três consultas numa só lista, que é ordenada e cortada no final.
 */
@Dao
interface AtividadeRecenteDao {
    @Query(
        "SELECT 'CHAMADA' AS origem, chamadas.id AS id, chamadas.updatedAt AS momento, " +
            "chamadas.createdAt AS criadoEm, NULL AS status, NULL AS nomeDoAluno, NULL AS texto, " +
            "(SELECT COUNT(*) FROM registros_presenca WHERE registros_presenca.chamadaId = chamadas.id " +
            "AND registros_presenca.presente = 0 AND registros_presenca.deletedAt IS NULL) AS ausentes " +
            "FROM chamadas WHERE chamadas.turmaId = :turmaId AND chamadas.deletedAt IS NULL " +

            "UNION ALL " +

            "SELECT 'PENDENCIA', pendencias.id, " +
            "CASE WHEN pendencias.entregueEm IS NOT NULL THEN pendencias.entregueEm ELSE pendencias.updatedAt END, " +
            "pendencias.createdAt, pendencias.status, alunos.nome, pendencias.descricao, NULL " +
            "FROM pendencias INNER JOIN alunos ON alunos.id = pendencias.alunoId " +
            "WHERE alunos.turmaId = :turmaId AND pendencias.deletedAt IS NULL AND alunos.deletedAt IS NULL " +

            "UNION ALL " +

            "SELECT 'ANOTACAO', anotacoes.id, anotacoes.updatedAt, anotacoes.createdAt, NULL, " +
            "alunos.nome, anotacoes.texto, NULL " +
            "FROM anotacoes INNER JOIN alunos ON alunos.id = anotacoes.alunoId " +
            "WHERE alunos.turmaId = :turmaId AND anotacoes.deletedAt IS NULL AND alunos.deletedAt IS NULL " +

            "ORDER BY momento DESC LIMIT :limite"
    )
    fun observarDaTurma(turmaId: String, limite: Int): Flow<List<LinhaDeAtividade>>
}
