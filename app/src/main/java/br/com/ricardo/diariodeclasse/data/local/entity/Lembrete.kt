package br.com.ricardo.diariodeclasse.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

/**
 * Algo que a própria professora precisa fazer até uma data (ex.: "Entregar o
 * portfólio socioemocional"). Diferente da pendência, não é de um aluno, e é
 * geral: não pertence a uma turma, aparece qualquer que seja a turma ativa.
 */
@Entity(
    tableName = "lembretes",
    indices = [Index("data")],
)
data class Lembrete(
    @PrimaryKey override val id: String,
    val descricao: String,
    /** Dia da entrega (ou do que precisa ser feito). */
    val data: LocalDate,
    /** Preenchido quando ela marca como feito; `null` = em aberto. */
    val concluidoEm: Instant? = null,
    override val createdAt: Instant,
    override val updatedAt: Instant,
    override val deletedAt: Instant? = null,
) : EntidadeBase
