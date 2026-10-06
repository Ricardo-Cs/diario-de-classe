package br.com.ricardo.diariodeclasse.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

/** Observação livre da professora sobre um aluno (as "anotações específicas" do caderno). */
@Entity(
    tableName = "anotacoes",
    foreignKeys = [
        ForeignKey(
            entity = Aluno::class,
            parentColumns = ["id"],
            childColumns = ["alunoId"],
        ),
    ],
    indices = [Index("alunoId")],
)
data class Anotacao(
    @PrimaryKey override val id: String,
    val alunoId: String,
    val texto: String,
    /** Dia a que a anotação se refere (no caderno, a data escrita ao lado). */
    val data: LocalDate,
    override val createdAt: Instant,
    override val updatedAt: Instant,
    override val deletedAt: Instant? = null,
) : EntidadeBase
