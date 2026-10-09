package br.com.ricardo.diariodeclasse.ui.perfil

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
 * Painel para editar nome e escola. Só o nome é obrigatório.
 *
 * `ModalBottomSheet` ainda é experimental no Material 3; o `@OptIn` fica só aqui.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolhaEditarPerfil(
    nomeInicial: String,
    escolaInicial: String,
    aoSalvar: (nome: String, escola: String) -> Unit,
    aoFechar: () -> Unit,
) {
    val nome: MutableState<String> = remember { mutableStateOf(nomeInicial) }
    val escola: MutableState<String> = remember { mutableStateOf(escolaInicial) }
    val focoNoNome: FocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focoNoNome.requestFocus()
    }

    // Nomes próprios: cada palavra começa com maiúscula.
    val comIniciaisMaiusculas = KeyboardOptions(capitalization = KeyboardCapitalization.Words)

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
                text = stringResource(R.string.perfil_editar),
                style = MaterialTheme.typography.titleMedium,
            )
            OutlinedTextField(
                value = nome.value,
                onValueChange = { novoNome -> nome.value = novoNome },
                label = { Text(stringResource(R.string.perfil_campo_nome)) },
                singleLine = true,
                keyboardOptions = comIniciaisMaiusculas,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focoNoNome),
            )
            OutlinedTextField(
                value = escola.value,
                onValueChange = { novaEscola -> escola.value = novaEscola },
                label = { Text(stringResource(R.string.perfil_campo_escola)) },
                singleLine = true,
                keyboardOptions = comIniciaisMaiusculas,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(Modifier.weight(1f))
                Button(
                    enabled = nome.value.isNotBlank(),
                    onClick = { aoSalvar(nome.value, escola.value) },
                ) {
                    Text(stringResource(R.string.salvar))
                }
            }
        }
    }
}
