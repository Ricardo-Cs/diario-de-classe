package br.com.ricardo.diariodeclasse.ui.fotos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import br.com.ricardo.diariodeclasse.R

/**
 * Painel para escrever a legenda da foto. Salvar com o campo vazio tira a
 * legenda, por isso o botão fica sempre habilitado.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolhaLegenda(
    legendaInicial: String?,
    aoSalvar: (legenda: String) -> Unit,
    aoFechar: () -> Unit,
) {
    var textoInicial = ""
    if (legendaInicial != null) {
        textoInicial = legendaInicial
    }
    val legenda: MutableState<String> = remember { mutableStateOf(textoInicial) }
    val focoNoCampo: FocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focoNoCampo.requestFocus()
    }

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
            Text(text = stringResource(R.string.foto_legenda), style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = legenda.value,
                onValueChange = { texto -> legenda.value = texto },
                placeholder = { Text(stringResource(R.string.foto_legenda_exemplo)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focoNoCampo),
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(Modifier.weight(1f))
                Button(onClick = { aoSalvar(legenda.value) }) {
                    Text(stringResource(R.string.salvar))
                }
            }
        }
    }
}
