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
