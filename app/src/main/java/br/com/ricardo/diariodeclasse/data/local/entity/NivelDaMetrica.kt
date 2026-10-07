package br.com.ricardo.diariodeclasse.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Um degrau da escala de uma métrica (ex.: "Silábico com valor sonoro").
 *
 * Os resultados apontam para o id do nível, não para o nome: renomear um nível
 * atualiza o histórico inteiro. Um nível removido recebe `deletedAt`, e os
 * resultados antigos continuam mostrando o nome dele.
 */
@Entity(
    tableName = "niveis_da_metrica",
    foreignKeys = [
        ForeignKey(
            entity = Metrica::class,
            parentColumns = ["id"],
            childColumns = ["metricaId"],
        ),
    ],
    indices = [Index("metricaId")],
)
data class NivelDaMetrica(
    @PrimaryKey override val id: String,
    val metricaId: String,
    val nome: String,
    /** Posição na escala, começando em 0 (o mais inicial). Define o que é "avançar". */
    val ordem: Int,
    override val createdAt: Instant,
    override val updatedAt: Instant,
    override val deletedAt: Instant? = null,
) : EntidadeBase
