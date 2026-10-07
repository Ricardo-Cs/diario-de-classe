package br.com.ricardo.diariodeclasse.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Algo que a professora acompanha em cada aluno ao longo do tempo (ex.: "Nível de
 * escrita"). Por enquanto toda métrica é uma escala de níveis ([NivelDaMetrica]);
 * quando surgir outro tipo (número, sim/não), entra uma coluna `tipo` com valor
 * padrão para as métricas já existentes.
 */
@Entity(
    tableName = "metricas",
    foreignKeys = [
        ForeignKey(
            entity = Turma::class,
            parentColumns = ["id"],
            childColumns = ["turmaId"],
        ),
    ],
    indices = [Index("turmaId")],
)
data class Metrica(
    @PrimaryKey override val id: String,
    val turmaId: String,
    val nome: String,
    override val createdAt: Instant,
    override val updatedAt: Instant,
    override val deletedAt: Instant? = null,
) : EntidadeBase
