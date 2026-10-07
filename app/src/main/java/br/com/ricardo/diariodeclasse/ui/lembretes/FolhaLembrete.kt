package br.com.ricardo.diariodeclasse.ui.lembretes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import br.com.ricardo.diariodeclasse.R
import br.com.ricardo.diariodeclasse.ui.componentes.DialogoCalendario
import br.com.ricardo.diariodeclasse.ui.componentes.formatarDataPorExtenso
import java.time.LocalDate

/**
 * Painel para criar ou editar um lembrete: o que fazer e até quando.
 * A data começa vazia na criação: uma entrega quase nunca é "hoje", e uma data
 * sugerida errada passaria despercebida. [aoExcluir] `null` = lembrete novo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolhaLembrete(
    titulo: String,
    descricaoInicial: String,
    dataInicial: LocalDate?,
    hoje: LocalDate,
    aoSalvar: (descricao: String, data: LocalDate) -> Unit,
    aoExcluir: (() -> Unit)?,
    aoFechar: () -> Unit,
) {
    val descricao: MutableState<String> = remember { mutableStateOf(descricaoInicial) }
    val data: MutableState<LocalDate?> = remember { mutableStateOf(dataInicial) }
    val escolhendoData: MutableState<Boolean> = remember { mutableStateOf(false) }

    val dataEscolhida: LocalDate? = data.value
    val podeSalvar: Boolean = descricao.value.isNotBlank() && dataEscolhida != null

    ModalBottomSheet(
        onDismissRequest = aoFechar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                .imePadding(),
        ) {
            Text(text = titulo, style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = descricao.value,
                onValueChange = { texto -> descricao.value = texto },
                label = { Text(stringResource(R.string.lembrete_campo_descricao)) },
                placeholder = { Text(stringResource(R.string.lembrete_campo_descricao_exemplo)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            val textoDaData: String
            if (dataEscolhida == null) {
                textoDaData = stringResource(R.string.lembrete_escolher_data)
            } else {
                textoDaData = formatarDataPorExtenso(dataEscolhida)
            }
            Column {
                Text(
                    text = stringResource(R.string.lembrete_campo_data),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(onClick = { escolhendoData.value = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(text = textoDaData, modifier = Modifier.weight(1f))
                }
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                if (aoExcluir != null) {
                    TextButton(
                        onClick = aoExcluir,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) {
                        Text(stringResource(R.string.excluir))
                    }
                }
                Spacer(Modifier.weight(1f))
                Button(
                    enabled = podeSalvar,
                    onClick = {
                        if (dataEscolhida != null) {
                            aoSalvar(descricao.value, dataEscolhida)
                        }
                    },
                ) {
                    Text(stringResource(R.string.salvar))
                }
            }
        }
    }

    if (escolhendoData.value) {
        var dataDoCalendario: LocalDate = hoje
        if (dataEscolhida != null) {
            dataDoCalendario = dataEscolhida
        }
        DialogoCalendario(
            dataInicial = dataDoCalendario,
            ultimaDataPermitida = null,
            aoEscolher = { escolhida ->
                data.value = escolhida
                escolhendoData.value = false
            },
            aoCancelar = { escolhendoData.value = false },
        )
    }
}
