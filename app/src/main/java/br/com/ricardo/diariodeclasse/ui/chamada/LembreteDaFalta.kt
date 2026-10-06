package br.com.ricardo.diariodeclasse.ui.chamada

import java.time.LocalDate

/**
 * Data sugerida para o lembrete das atividades de quem faltou: o dia seguinte
 * à falta, quando o aluno normalmente volta. Numa chamada registrada depois
 * (ex.: a de ontem, feita hoje), esse dia já passou; então a sugestão é hoje.
 */
fun lembreteInicialDaFalta(dataDaChamada: LocalDate, hoje: LocalDate): LocalDate {
    val diaSeguinteAFalta: LocalDate = dataDaChamada.plusDays(1)
    if (diaSeguinteAFalta.isBefore(hoje)) {
        return hoje
    }
    return diaSeguinteAFalta
}
