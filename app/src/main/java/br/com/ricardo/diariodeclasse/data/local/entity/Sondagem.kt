package br.com.ricardo.diariodeclasse.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

/**
 * Uma rodada de avaliação da turma numa métrica, numa data. Funciona como a
 * [Chamada]: o índice único impede duas sondagens da mesma métrica no mesmo dia,
 * e editar reaproveita a existente.
 */
@Entity(
    tableName = "sondagens",
    foreignKeys = [
        ForeignKey(
            entity = Metrica::class,
            parentColumns = ["id"],
            childColumns = ["metricaId"],
        ),
    ],
    indices = [Index(value = ["metricaId", "data"], unique = true)],
)
data class Sondagem(
    @PrimaryKey override val id: String,
    val metricaId: String,
    val data: LocalDate,
    override val createdAt: Instant,
    override val updatedAt: Instant,
    override val deletedAt: Instant? = null,
) : EntidadeBase
