package br.com.ricardo.diariodeclasse.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

/**
 * Uma foto do registro do dia da turma. A imagem fica num arquivo na pasta
 * privada do app; o banco guarda só o nome desse arquivo.
 *
 * Guardamos o nome, e não o caminho completo, porque o caminho muda de um
 * aparelho para outro (e numa importação), enquanto o nome continua o mesmo.
 */
@Entity(
    tableName = "fotos",
    foreignKeys = [
        ForeignKey(
            entity = Turma::class,
            parentColumns = ["id"],
            childColumns = ["turmaId"],
        ),
    ],
    indices = [Index("turmaId", "data")],
)
data class Foto(
    @PrimaryKey override val id: String,
    val turmaId: String,
    /** Dia da aula a que a foto pertence (pode ser diferente do dia em que foi adicionada). */
    val data: LocalDate,
    /** Ex.: "3f2a...c1.jpg", dentro da pasta de fotos do app. */
    val nomeDoArquivo: String,
    /** Texto curto opcional, ex.: "Pintura com guache". */
    val legenda: String? = null,
    override val createdAt: Instant,
    override val updatedAt: Instant,
    override val deletedAt: Instant? = null,
) : EntidadeBase
