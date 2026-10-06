package br.com.ricardo.diariodeclasse.ui.pendencias

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import br.com.ricardo.diariodeclasse.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** "falta em 06/10" para pendências criadas na chamada; "avulsa" para as demais. */
@Composable
fun textoDaOrigem(dataDaFalta: LocalDate?): String {
    if (dataDaFalta == null) {
        return stringResource(R.string.pendencia_origem_avulsa)
    }
    val dataCurta: String = dataDaFalta.format(DateTimeFormatter.ofPattern("dd/MM"))
    return stringResource(R.string.pendencia_origem_falta, dataCurta)
}
