package br.com.ricardo.diariodeclasse.ui.alunos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import br.com.ricardo.diariodeclasse.R

/**
 * Cadastro em sequência: a professora digita um nome, toca "Adicionar" (ou Enter no teclado),
 * o campo limpa e já está pronto para o próximo. O painel só fecha em "Concluir"
 * ou arrastando para baixo.
 *
 * `ModalBottomSheet` ainda é experimental no Material 3; o `@OptIn` fica só aqui.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolhaAdicionarAlunos(
    aoAdicionar: (nome: String) -> Unit,
    aoConcluir: () -> Unit,
) {
    val nomeDigitado: MutableState<String> = remember { mutableStateOf("") }
    val ultimoAdicionado: MutableState<String?> = remember { mutableStateOf(null) }
    val focoNoCampo: FocusRequester = remember { FocusRequester() }

    fun adicionar() {
        if (nomeDigitado.value.isBlank()) {
            return
        }
        aoAdicionar(nomeDigitado.value)
        ultimoAdicionado.value = nomeDigitado.value.trim()
        nomeDigitado.value = ""
    }

    LaunchedEffect(Unit) {
        focoNoCampo.requestFocus()
    }

    ModalBottomSheet(
        onDismissRequest = aoConcluir,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                .imePadding(),
        ) {
            Text(
                text = stringResource(R.string.aluno_adicionar_titulo),
                style = MaterialTheme.typography.titleMedium,
            )

            OutlinedTextField(
                value = nomeDigitado.value,
                onValueChange = { texto -> nomeDigitado.value = texto },
                label = { Text(stringResource(R.string.aluno_campo_nome)) },
                supportingText = { MensagemUltimoAdicionado(ultimoAdicionado.value) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { adicionar() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focoNoCampo),
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = aoConcluir) {
                    Text(stringResource(R.string.concluir))
                }
                Spacer(Modifier.weight(1f))
                Button(onClick = { adicionar() }, enabled = nomeDigitado.value.isNotBlank()) {
                    Text(stringResource(R.string.aluno_adicionar))
                }
            }
        }
    }
}

@Composable
private fun MensagemUltimoAdicionado(nome: String?) {
    if (nome != null) {
        Text(stringResource(R.string.aluno_adicionado, nome))
    }
}
