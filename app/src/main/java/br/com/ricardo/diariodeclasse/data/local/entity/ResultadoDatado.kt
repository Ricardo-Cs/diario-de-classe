package br.com.ricardo.diariodeclasse.data.local.entity

import java.time.LocalDate

/**
 * Um resultado de sondagem junto com a data e a métrica da sondagem. Não é tabela:
 * é o formato que as consultas com JOIN devolvem (os nomes das colunas do SELECT
 * precisam bater com os nomes das propriedades).
 */
data class ResultadoDatado(
    val metricaId: String,
    val alunoId: String,
    val nivelId: String,
    val data: LocalDate,
)
