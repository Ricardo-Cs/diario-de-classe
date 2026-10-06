package br.com.ricardo.diariodeclasse.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

enum class StatusPendencia { PENDENTE, ENTREGUE }

/**
 * Atividade que um aluno precisa fazer depois ("colocar na pasta").
 * Pode nascer de uma falta ([registroPresencaId]) ou ser avulsa, por exemplo
 * quando o aluno não terminou a atividade em sala.
 */
@Entity(
    tableName = "pendencias",
    foreignKeys = [
        ForeignKey(
            entity = Aluno::class,
            parentColumns = ["id"],
            childColumns = ["alunoId"],
        ),
        ForeignKey(
            entity = RegistroPresenca::class,
            parentColumns = ["id"],
            childColumns = ["registroPresencaId"],
        ),
    ],
    indices = [Index("alunoId"), Index("registroPresencaId")],
)
data class Pendencia(
    @PrimaryKey override val id: String,
    val alunoId: String,
    val descricao: String,
    /** Dia a partir do qual a pendência aparece como "para hoje" (e, no futuro, gera notificação). */
    val dataLembrete: LocalDate,
    val status: StatusPendencia,
    val registroPresencaId: String? = null,
    val entregueEm: Instant? = null,
    override val createdAt: Instant,
    override val updatedAt: Instant,
    override val deletedAt: Instant? = null,
) : EntidadeBase {

    /** Pendente e com o lembrete já vencido (hoje ou antes). */
    fun estaPendenteEm(data: LocalDate): Boolean {
        if (status != StatusPendencia.PENDENTE) {
            return false
        }
        return !dataLembrete.isAfter(data)
    }
}
