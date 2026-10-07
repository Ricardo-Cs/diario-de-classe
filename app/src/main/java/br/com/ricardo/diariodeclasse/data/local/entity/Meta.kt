package br.com.ricardo.diariodeclasse.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

/**
 * Meta da turma ligada a uma métrica: "estes alunos chegam ao [nivelAlvoId] até o [prazo]".
 *
 * O progresso não é gravado: é calculado a partir das sondagens, então nunca
 * fica desatualizado.
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
    val metricaId: String,
    /** O aluno atinge a meta ao chegar a este nível ou a um acima dele. */
    val nivelAlvoId: String,
    val prazo: LocalDate,
    /** Preenchido quando a professora encerra a meta. Não é exclusão: ela continua consultável. */
    val encerradaEm: LocalDate? = null,
    override val createdAt: Instant,
    override val updatedAt: Instant,
    override val deletedAt: Instant? = null,
) : EntidadeBase
