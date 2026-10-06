package br.com.ricardo.diariodeclasse.ui.pendencias

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.com.ricardo.diariodeclasse.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** Atalhos "Hoje" e "Amanhã" cobrem a maioria dos casos; "Outra data" abre o calendário. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EscolhaDoLembrete(
    hoje: LocalDate,
    dataEscolhida: LocalDate,
    aoEscolher: (data: LocalDate) -> Unit,
) {
    val amanha: LocalDate = hoje.plusDays(1)
    val ehHoje: Boolean = dataEscolhida == hoje
    val ehAmanha: Boolean = dataEscolhida == amanha
    val ehOutraData: Boolean = !ehHoje && !ehAmanha
    val calendarioAberto: MutableState<Boolean> = remember { mutableStateOf(false) }

    val textoOutraData: String
    if (ehOutraData) {
        textoOutraData = dataEscolhida.format(DateTimeFormatter.ofPattern("dd/MM"))
    } else {
        textoOutraData = stringResource(R.string.pendencia_outra_data)
    }

    Column {
        Text(
            text = stringResource(R.string.pendencia_lembrar),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = ehHoje,
                onClick = { aoEscolher(hoje) },
                label = { Text(stringResource(R.string.pendencia_hoje)) },
            )
            FilterChip(
                selected = ehAmanha,
                onClick = { aoEscolher(amanha) },
                label = { Text(stringResource(R.string.pendencia_amanha)) },
            )
            FilterChip(
                selected = ehOutraData,
                onClick = { calendarioAberto.value = true },
                label = { Text(textoOutraData) },
            )
        }
    }

    if (calendarioAberto.value) {
        DialogoCalendario(
            dataInicial = dataEscolhida,
            aoEscolher = { data ->
                aoEscolher(data)
                calendarioAberto.value = false
            },
            aoCancelar = { calendarioAberto.value = false },
        )
    }
}

/**
 * O `DatePicker` trabalha com milissegundos em UTC (meia-noite do dia escolhido),
 * por isso as conversões usam `ZoneOffset.UTC` e não o fuso do aparelho.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogoCalendario(
    dataInicial: LocalDate,
    aoEscolher: (data: LocalDate) -> Unit,
    aoCancelar: () -> Unit,
) {
    val milissegundosIniciais: Long = dataInicial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val estadoDoCalendario: DatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = milissegundosIniciais,
    )

    DatePickerDialog(
        onDismissRequest = aoCancelar,
        confirmButton = {
            TextButton(
                onClick = {
                    val milissegundos: Long? = estadoDoCalendario.selectedDateMillis
                    if (milissegundos != null) {
                        val data: LocalDate = Instant.ofEpochMilli(milissegundos).atZone(ZoneOffset.UTC).toLocalDate()
                        aoEscolher(data)
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
