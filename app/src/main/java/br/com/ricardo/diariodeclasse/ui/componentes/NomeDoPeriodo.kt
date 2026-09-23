package br.com.ricardo.diariodeclasse.ui.componentes

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import br.com.ricardo.diariodeclasse.R
import br.com.ricardo.diariodeclasse.data.local.entity.Periodo

@Composable
fun nomeDoPeriodo(periodo: Periodo): String {
    val idDoTexto: Int = when (periodo) {
        Periodo.MANHA -> R.string.periodo_manha
        Periodo.TARDE -> R.string.periodo_tarde
        Periodo.NOITE -> R.string.periodo_noite
        Periodo.INTEGRAL -> R.string.periodo_integral
    }
    return stringResource(idDoTexto)
}
