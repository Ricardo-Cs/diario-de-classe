package br.com.ricardo.diariodeclasse.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

/**
 * Aluno acompanhado por uma meta. A lista é gravada (em vez de calculada por um
 * filtro) para a meta não mudar sozinha quando um aluno avança de nível.
 */
@Entity(
    tableName = "alunos_na_meta",
    foreignKeys = [
        ForeignKey(
            entity = Meta::class,
            parentColumns = ["id"],
            childColumns = ["metaId"],
        ),
        ForeignKey(
            entity = Aluno::class,
            parentColumns = ["id"],
            childColumns = ["alunoId"],
        ),
        ForeignKey(
            entity = NivelDaMetrica::class,
            parentColumns = ["id"],
            childColumns = ["nivelInicialId"],
        ),
    ],
    indices = [
        Index(value = ["metaId", "alunoId"], unique = true),
        Index("alunoId"),
        Index("nivelInicialId"),
    ],
)
data class AlunoNaMeta(
    @PrimaryKey override val id: String,
    val metaId: String,
    val alunoId: String,
    /**
     * Nível do aluno quando entrou na meta (`null` se ainda não tinha sido avaliado).
     * É a referência para saber se ele "avançou" mesmo sem ter atingido o alvo.
     */
    val nivelInicialId: String?,
    /**
     * Dia em que a professora marcou que o aluno atingiu a meta. Só é usado nas
     * metas acompanhadas à mão; nas metas por métrica, quem diz isso são as sondagens.
     */
    val atingiuEm: LocalDate? = null,
    override val createdAt: Instant,
    override val updatedAt: Instant,
    override val deletedAt: Instant? = null,
) : EntidadeBase
