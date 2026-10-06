package br.com.ricardo.diariodeclasse.ui.componentes

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import br.com.ricardo.diariodeclasse.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Calendário em diálogo. Com [ultimaDataPermitida], os dias depois dela ficam
 * desabilitados (ex.: não existe chamada de amanhã).
 *
 * O `DatePicker` trabalha com milissegundos em UTC (meia-noite do dia escolhido),
 * por isso as conversões usam `ZoneOffset.UTC` e não o fuso do aparelho.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogoCalendario(
    dataInicial: LocalDate,
    ultimaDataPermitida: LocalDate?,
    aoEscolher: (data: LocalDate) -> Unit,
    aoCancelar: () -> Unit,
) {
    val datasPermitidas: SelectableDates
    if (ultimaDataPermitida == null) {
        datasPermitidas = DatePickerDefaults.AllDates
    } else {
        datasPermitidas = DatasAte(ultimaDataPermitida)
    }

    val estadoDoCalendario: DatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = paraMilissegundos(dataInicial),
        selectableDates = datasPermitidas,
    )

    DatePickerDialog(
        onDismissRequest = aoCancelar,
        confirmButton = {
            TextButton(
                onClick = {
                    val milissegundos: Long? = estadoDoCalendario.selectedDateMillis
                    if (milissegundos != null) {
                        aoEscolher(paraData(milissegundos))
                    }
                },
            ) {
                Text(stringResource(R.string.concluir))
            }
        },
        dismissButton = {
            TextButton(onClick = aoCancelar) {
                Text(stringResource(R.string.cancelar))
            }
        },
    ) {
        DatePicker(state = estadoDoCalendario)
    }
}

/**
 * Regra do calendário: só os dias até [ultimaData] (inclusive) podem ser tocados.
 * `SelectableDates` é uma interface do Material 3; o calendário pergunta a ela,
 * dia a dia, se o dia fica habilitado.
 */
@OptIn(ExperimentalMaterial3Api::class)
private class DatasAte(private val ultimaData: LocalDate) : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
        val data: LocalDate = paraData(utcTimeMillis)
        return !data.isAfter(ultimaData)
    }

    override fun isSelectableYear(year: Int): Boolean {
        return year <= ultimaData.year
    }
}

private fun paraMilissegundos(data: LocalDate): Long {
    return data.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}

private fun paraData(milissegundos: Long): LocalDate {
    return Instant.ofEpochMilli(milissegundos).atZone(ZoneOffset.UTC).toLocalDate()
}
