package br.com.ricardo.diariodeclasse.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Situação de um aluno em uma chamada. Guardamos também os presentes para saber
 * quem fazia parte da turma naquele dia (alunos cadastrados depois não contam como falta).
 */
@Entity(
    tableName = "registros_presenca",
    foreignKeys = [
        ForeignKey(
            entity = Chamada::class,
            parentColumns = ["id"],
            childColumns = ["chamadaId"],
        ),
        ForeignKey(
            entity = Aluno::class,
            parentColumns = ["id"],
            childColumns = ["alunoId"],
        ),
    ],
    indices = [
        Index(value = ["chamadaId", "alunoId"], unique = true),
        Index("alunoId"),
    ],
)
data class RegistroPresenca(
    @PrimaryKey override val id: String,
    val chamadaId: String,
    val alunoId: String,
    val presente: Boolean,
    /** Ex.: "atestado", "família avisou". Só faz sentido em faltas. */
    val observacao: String?,
    override val createdAt: Instant,
    override val updatedAt: Instant,
    override val deletedAt: Instant? = null,
) : EntidadeBase
