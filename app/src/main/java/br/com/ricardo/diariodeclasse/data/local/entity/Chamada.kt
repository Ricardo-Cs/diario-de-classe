package br.com.ricardo.diariodeclasse.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

/**
 * Registro de faltas de uma turma em um dia. O índice único impede duas
 * chamadas para a mesma turma na mesma data: editar reaproveita a existente.
 */
@Entity(
    tableName = "chamadas",
    foreignKeys = [
        ForeignKey(
            entity = Turma::class,
            parentColumns = ["id"],
            childColumns = ["turmaId"],
        ),
    ],
    indices = [Index(value = ["turmaId", "data"], unique = true)],
)
data class Chamada(
    @PrimaryKey override val id: String,
    val turmaId: String,
    val data: LocalDate,
    override val createdAt: Instant,
    override val updatedAt: Instant,
    override val deletedAt: Instant? = null,
) : EntidadeBase
