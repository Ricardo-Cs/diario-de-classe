package br.com.ricardo.diariodeclasse.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/** Só o nome: a LGPD pede o mínimo necessário de dados das crianças. */
@Entity(
    tableName = "alunos",
    foreignKeys = [
        ForeignKey(
            entity = Turma::class,
            parentColumns = ["id"],
            childColumns = ["turmaId"],
        ),
    ],
    indices = [Index("turmaId")],
)
data class Aluno(
    @PrimaryKey override val id: String,
    val turmaId: String,
    val nome: String,
    override val createdAt: Instant,
    override val updatedAt: Instant,
    override val deletedAt: Instant? = null,
) : EntidadeBase
