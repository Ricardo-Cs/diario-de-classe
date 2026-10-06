package br.com.ricardo.diariodeclasse.ui.componentes

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import br.com.ricardo.diariodeclasse.R
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

/**
 * Ex.: "Seg., 05/10" ou, se for de outro ano, "Seg., 15/12/2025". O dia da semana
 * ajuda a ver padrões (ex.: faltas sempre às segundas).
 */
fun formatarDataComDiaDaSemana(data: LocalDate, hoje: LocalDate): String {
    val padrao: String
    if (data.year == hoje.year) {
        padrao = "EEE, dd/MM"
    } else {
        padrao = "EEE, dd/MM/yyyy"
    }
    val texto: String = data.format(DateTimeFormatter.ofPattern(padrao, PORTUGUES_DO_BRASIL))

    val primeiraLetraMaiuscula: String = texto.substring(0, 1).uppercase(PORTUGUES_DO_BRASIL)
    return primeiraLetraMaiuscula + texto.substring(1)
}

/** Ex.: "outubro". */
fun nomeDoMes(data: LocalDate): String {
    return data.format(DateTimeFormatter.ofPattern("MMMM", PORTUGUES_DO_BRASIL))
}

/** "Hoje", "Ontem", "02/10" ou, se for de outro ano, "15/12/2025". */
@Composable
fun textoDeDataRelativa(data: LocalDate, hoje: LocalDate): String {
    if (data == hoje) {
        return stringResource(R.string.data_hoje)
    }
    if (data == hoje.minusDays(1)) {
        return stringResource(R.string.data_ontem)
    }
    if (data.year == hoje.year) {
        return data.format(DateTimeFormatter.ofPattern("dd/MM"))
    }
    return data.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
}
