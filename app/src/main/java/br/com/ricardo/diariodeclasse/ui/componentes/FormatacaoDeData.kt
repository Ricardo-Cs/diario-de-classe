package br.com.ricardo.diariodeclasse.ui.componentes

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val PORTUGUES_DO_BRASIL: Locale = Locale.forLanguageTag("pt-BR")

/** Ex.: "Terça-feira, 6 de outubro". */
fun formatarDataPorExtenso(data: LocalDate): String {
    val formato: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", PORTUGUES_DO_BRASIL)
    val texto: String = data.format(formato)

    val primeiraLetraMaiuscula: String = texto.substring(0, 1).uppercase(PORTUGUES_DO_BRASIL)
    return primeiraLetraMaiuscula + texto.substring(1)
}
