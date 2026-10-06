package br.com.ricardo.diariodeclasse.ui.pendencias

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.com.ricardo.diariodeclasse.R
import br.com.ricardo.diariodeclasse.ui.componentes.DialogoCalendario
import java.time.LocalDate
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
            ultimaDataPermitida = null,
            aoEscolher = { data ->
                aoEscolher(data)
                calendarioAberto.value = false
            },
            aoCancelar = { calendarioAberto.value = false },
        )
    }
}
