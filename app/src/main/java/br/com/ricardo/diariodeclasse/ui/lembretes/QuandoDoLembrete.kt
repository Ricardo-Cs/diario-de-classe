package br.com.ricardo.diariodeclasse.ui.lembretes

import br.com.ricardo.diariodeclasse.data.local.entity.Lembrete
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Quantos dias antes da data o lembrete passa a aparecer no Início. */
const val DIAS_DE_ANTECEDENCIA_NO_INICIO: Long = 7

/** Situação do lembrete em relação a hoje, para escolher o texto e a cor. */
sealed interface QuandoDoLembrete {
    data class Atrasado(val dias: Int) : QuandoDoLembrete
    data object Hoje : QuandoDoLembrete
    data object Amanha : QuandoDoLembrete
    data class EmDias(val dias: Int) : QuandoDoLembrete
}

fun quandoDoLembrete(data: LocalDate, hoje: LocalDate): QuandoDoLembrete {
    val dias: Int = ChronoUnit.DAYS.between(hoje, data).toInt()
    if (dias < 0) {
        return QuandoDoLembrete.Atrasado(dias = -dias)
    }
    if (dias == 0) {
        return QuandoDoLembrete.Hoje
    }
    if (dias == 1) {
        return QuandoDoLembrete.Amanha
    }
    return QuandoDoLembrete.EmDias(dias)
}

/**
 * Os lembretes em aberto que aparecem no Início: os atrasados, os de hoje e os
 * dos próximos [DIAS_DE_ANTECEDENCIA_NO_INICIO] dias. Os mais distantes ficam só
 * na tela de lembretes, para o Início mostrar o que pede atenção agora.
 * Mantém a ordem recebida (por data).
 */
fun lembretesDoInicio(emAberto: List<Lembrete>, hoje: LocalDate): List<Lembrete> {
    val limite: LocalDate = hoje.plusDays(DIAS_DE_ANTECEDENCIA_NO_INICIO)
    val doInicio = mutableListOf<Lembrete>()
    for (lembrete in emAberto) {
        if (!lembrete.data.isAfter(limite)) {
            doInicio.add(lembrete)
        }
    }
    return doInicio
}
