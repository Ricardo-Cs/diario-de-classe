package br.com.ricardo.diariodeclasse.notificacao

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** A partir deste horário a notificação do dia pode aparecer. */
val HORARIO_DO_LEMBRETE: LocalTime = LocalTime.of(7, 0)

/**
 * A verificação roda de hora em hora; avisa na primeira vez depois do
 * [HORARIO_DO_LEMBRETE] de um dia útil que ainda não foi avisado.
 *
 * Fim de semana fica de fora: as pendências com lembrete no sábado ou no
 * domingo continuam em aberto e entram no aviso de segunda.
 */
fun deveAvisarAgora(agora: LocalDateTime, ultimoDiaAvisado: LocalDate?): Boolean {
    val hoje: LocalDate = agora.toLocalDate()

    val diaDaSemana: DayOfWeek = hoje.dayOfWeek
    if (diaDaSemana == DayOfWeek.SATURDAY || diaDaSemana == DayOfWeek.SUNDAY) {
        return false
    }
    if (agora.toLocalTime().isBefore(HORARIO_DO_LEMBRETE)) {
        return false
    }
    if (ultimoDiaAvisado == hoje) {
        return false
    }
    return true
}
