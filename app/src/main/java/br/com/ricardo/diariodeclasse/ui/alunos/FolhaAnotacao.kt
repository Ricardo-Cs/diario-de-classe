package br.com.ricardo.diariodeclasse.ui.alunos

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
 * Painel para escrever ou editar uma anotação. O campo aceita várias linhas,
 * como no caderno. [aoExcluir] `null` = anotação nova (sem botão "Excluir").
 *
 * `ModalBottomSheet` ainda é experimental no Material 3; o `@OptIn` fica só aqui.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolhaAnotacao(
    titulo: String,
    textoInicial: String,
    aoSalvar: (texto: String) -> Unit,
    aoExcluir: (() -> Unit)?,
    aoFechar: () -> Unit,
) {
    val texto: MutableState<String> = remember { mutableStateOf(textoInicial) }
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
            Text(text = titulo, style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = texto.value,
                onValueChange = { novoTexto -> texto.value = novoTexto },
                label = { Text(stringResource(R.string.anotacao_campo_texto)) },
                placeholder = { Text(stringResource(R.string.anotacao_campo_texto_exemplo)) },
                minLines = 3,
                maxLines = 8,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focoNoCampo),
            )

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
                    enabled = texto.value.isNotBlank(),
                    onClick = { aoSalvar(texto.value) },
                ) {
                    Text(stringResource(R.string.salvar))
                }
            }
        }
    }
}
