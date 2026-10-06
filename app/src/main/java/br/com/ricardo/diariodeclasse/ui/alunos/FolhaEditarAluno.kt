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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import br.com.ricardo.diariodeclasse.R
import br.com.ricardo.diariodeclasse.data.local.entity.Aluno

/**
 * Painel para renomear ou excluir o aluno, no mesmo formato do painel de
 * anotação: "Excluir" à esquerda, em vermelho, e "Salvar" à direita.
 * Fechar arrastando para baixo descarta a edição.
 *
 * `ModalBottomSheet` ainda é experimental no Material 3; o `@OptIn` fica só aqui.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolhaEditarAluno(
    aluno: Aluno,
    aoSalvar: (novoNome: String) -> Unit,
    aoExcluir: () -> Unit,
    aoFechar: () -> Unit,
) {
    val nomeDigitado: MutableState<String> = remember { mutableStateOf(aluno.nome) }

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
            Text(
                text = stringResource(R.string.aluno_editar_titulo),
                style = MaterialTheme.typography.titleMedium,
            )

            OutlinedTextField(
                value = nomeDigitado.value,
                onValueChange = { texto -> nomeDigitado.value = texto },
                label = { Text(stringResource(R.string.aluno_campo_nome)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    onClick = aoExcluir,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(stringResource(R.string.excluir))
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = { aoSalvar(nomeDigitado.value) },
                    enabled = nomeDigitado.value.isNotBlank(),
                ) {
                    Text(stringResource(R.string.salvar))
                }
            }
        }
    }
}
