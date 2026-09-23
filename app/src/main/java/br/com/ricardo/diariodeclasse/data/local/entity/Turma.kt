package br.com.ricardo.diariodeclasse.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

enum class Periodo { MANHA, TARDE, NOITE, INTEGRAL }

@Entity(tableName = "turmas")
data class Turma(
    @PrimaryKey override val id: String,
    val nome: String,
    val anoSerie: String,
    val periodo: Periodo,
    val anoLetivo: Int,
    override val createdAt: Instant,
    override val updatedAt: Instant,
    override val deletedAt: Instant? = null,
) : EntidadeBase
