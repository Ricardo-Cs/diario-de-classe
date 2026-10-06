package br.com.ricardo.diariodeclasse.data.local.entity

import androidx.room.Embedded
import java.time.LocalDate

/**
 * Resultado de consulta, não é tabela: a pendência mais a data da falta que a
 * originou (vem da chamada, via JOIN). `dataDaFalta == null` = pendência avulsa.
 *
 * `@Embedded` diz ao Room que as colunas da pendência vêm "achatadas" no mesmo
 * resultado e devem ser montadas de volta num objeto [Pendencia].
 */
data class PendenciaComOrigem(
    @Embedded val pendencia: Pendencia,
    val dataDaFalta: LocalDate?,
)
