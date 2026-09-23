package br.com.ricardo.diariodeclasse.ui.alunos

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import br.com.ricardo.diariodeclasse.R
import br.com.ricardo.diariodeclasse.data.local.entity.Aluno

@Composable
fun DialogoEditarAluno(
    aluno: Aluno,
    aoSalvar: (novoNome: String) -> Unit,
    aoExcluir: () -> Unit,
    aoCancelar: () -> Unit,
) {
    val nomeDigitado: MutableState<String> = remember { mutableStateOf(aluno.nome) }

    AlertDialog(
        onDismissRequest = aoCancelar,
        title = { Text(stringResource(R.string.aluno_editar_titulo)) },
        text = {
            OutlinedTextField(
                value = nomeDigitado.value,
                onValueChange = { texto -> nomeDigitado.value = texto },
                label = { Text(stringResource(R.string.aluno_campo_nome)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { aoSalvar(nomeDigitado.value) },
                enabled = nomeDigitado.value.isNotBlank(),
            ) {
                Text(stringResource(R.string.salvar))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = aoExcluir) {
                    Text(
                        text = stringResource(R.string.excluir),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                TextButton(onClick = aoCancelar) {
                    Text(stringResource(R.string.cancelar))
                }
            }
        },
    )
}
