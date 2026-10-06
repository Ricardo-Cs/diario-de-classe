package br.com.ricardo.diariodeclasse.data.local.entity

import java.time.LocalDate

/**
 * Resultado de consulta, não é tabela: um dia em que o aluno faltou (a data vem
 * da chamada, via JOIN) e a observação registrada na falta, se houver.
 */
data class FaltaDoAluno(
    val data: LocalDate,
    val observacao: String?,
)
