package br.com.ricardo.diariodeclasse.ui.alunos

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import br.com.ricardo.diariodeclasse.R

/**
 * Cadastro em sequência: a professora digita um nome, toca "Adicionar" (ou Enter no teclado),
 * o campo limpa e já está pronto para o próximo. O diálogo só fecha em "Concluir".
 */
@Composable
fun DialogoAdicionarAlunos(
    aoAdicionar: (nome: String) -> Unit,
    aoConcluir: () -> Unit,
) {
    val nomeDigitado: MutableState<String> = remember { mutableStateOf("") }
    val ultimoAdicionado: MutableState<String?> = remember { mutableStateOf(null) }
    val focoDoCampo: FocusRequester = remember { FocusRequester() }

    fun adicionar() {
        if (nomeDigitado.value.isBlank()) {
            return
        }
        aoAdicionar(nomeDigitado.value)
        ultimoAdicionado.value = nomeDigitado.value.trim()
        nomeDigitado.value = ""
    }

    LaunchedEffect(Unit) {
        focoDoCampo.requestFocus()
    }

    AlertDialog(
        onDismissRequest = aoConcluir,
        title = { Text(stringResource(R.string.aluno_adicionar_titulo)) },
        text = {
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
                modifier = Modifier.focusRequester(focoDoCampo),
            )
        },
        confirmButton = {
            TextButton(onClick = { adicionar() }, enabled = nomeDigitado.value.isNotBlank()) {
                Text(stringResource(R.string.aluno_adicionar))
            }
        },
        dismissButton = {
            TextButton(onClick = aoConcluir) {
                Text(stringResource(R.string.concluir))
            }
        },
    )
}

@Composable
private fun MensagemUltimoAdicionado(nome: String?) {
    if (nome != null) {
        Text(stringResource(R.string.aluno_adicionado, nome))
    }
}
