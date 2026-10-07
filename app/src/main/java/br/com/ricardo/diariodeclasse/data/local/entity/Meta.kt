package br.com.ricardo.diariodeclasse.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

/**
 * Meta da turma sobre um grupo de alunos ("conhecer a família numérica do 10 ao 80").
 * Há duas formas de acompanhar, escolhidas na criação e fixas depois:
 * - marcando à mão: [metricaId] e [nivelAlvoId] ficam `null` e a professora marca
 *   quem atingiu (ver [AlunoNaMeta.atingiuEm]);
 * - pela métrica: "estes alunos chegam ao [nivelAlvoId]". O progresso não é gravado,
 *   é calculado a partir das sondagens, então nunca fica desatualizado.
 */
@Entity(
    tableName = "metas",
    foreignKeys = [
        ForeignKey(
            entity = Turma::class,
            parentColumns = ["id"],
            childColumns = ["turmaId"],
        ),
        ForeignKey(
            entity = Metrica::class,
            parentColumns = ["id"],
            childColumns = ["metricaId"],
        ),
        ForeignKey(
            entity = NivelDaMetrica::class,
            parentColumns = ["id"],
            childColumns = ["nivelAlvoId"],
        ),
    ],
    indices = [Index("turmaId"), Index("metricaId"), Index("nivelAlvoId")],
)
data class Meta(
    @PrimaryKey override val id: String,
    val turmaId: String,
    val descricao: String,
    /** `null` = meta acompanhada marcando à mão. */
    val metricaId: String?,
    /** O aluno atinge a meta ao chegar a este nível ou a um acima dele. `null` junto com [metricaId]. */
    val nivelAlvoId: String?,
    /** Opcional: nem toda meta tem data definida. */
    val prazo: LocalDate?,
    /** Preenchido quando a professora encerra a meta. Não é exclusão: ela continua consultável. */
    val encerradaEm: LocalDate? = null,
    override val createdAt: Instant,
    override val updatedAt: Instant,
    override val deletedAt: Instant? = null,
) : EntidadeBase {

    fun acompanhadaPorMetrica(): Boolean {
        return metricaId != null
    }
}
