package br.com.ricardo.diariodeclasse.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Nível em que um aluno estava numa sondagem. Aluno marcado como "não avaliado"
 * não tem linha (ou tem a linha com `deletedAt`, se antes tinha sido avaliado).
 */
@Entity(
    tableName = "resultados_da_sondagem",
    foreignKeys = [
        ForeignKey(
            entity = Sondagem::class,
            parentColumns = ["id"],
            childColumns = ["sondagemId"],
        ),
        ForeignKey(
            entity = Aluno::class,
            parentColumns = ["id"],
            childColumns = ["alunoId"],
        ),
        ForeignKey(
            entity = NivelDaMetrica::class,
            parentColumns = ["id"],
            childColumns = ["nivelId"],
        ),
    ],
    indices = [
        Index(value = ["sondagemId", "alunoId"], unique = true),
        Index("alunoId"),
        Index("nivelId"),
    ],
)
data class ResultadoDaSondagem(
    @PrimaryKey override val id: String,
    val sondagemId: String,
    val alunoId: String,
    val nivelId: String,
    override val createdAt: Instant,
    override val updatedAt: Instant,
    override val deletedAt: Instant? = null,
) : EntidadeBase
