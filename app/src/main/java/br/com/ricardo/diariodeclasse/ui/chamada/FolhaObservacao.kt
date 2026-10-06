package br.com.ricardo.diariodeclasse.ui.chamada

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
import androidx.compose.material3.SuggestionChip
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
 * Painel que sobe da parte de baixo da tela para escrever a observação de uma falta.
 * O texto fica num rascunho local e só vai para o ViewModel ao tocar em "Concluir";
 * fechar o painel arrastando para baixo descarta a edição.
 *
 * O `ModalBottomSheet` do Material 3 ainda é marcado como experimental; o `@OptIn` fica só aqui.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolhaObservacao(
    nomeDoAluno: String,
    observacaoAtual: String,
    aoConcluir: (observacao: String) -> Unit,
    aoFechar: () -> Unit,
) {
    val rascunho: MutableState<String> = remember { mutableStateOf(observacaoAtual) }
    val focoNoCampo: FocusRequester = remember { FocusRequester() }
    val jaTinhaObservacao: Boolean = observacaoAtual.isNotBlank()

    // Abre o teclado direto no campo: quem tocou em "Adicionar observação" quer escrever.
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
            Text(
                text = stringResource(R.string.chamada_observacao_de, nomeDoAluno),
                style = MaterialTheme.typography.titleMedium,
            )

            OutlinedTextField(
                value = rascunho.value,
                onValueChange = { texto -> rascunho.value = texto },
                label = { Text(stringResource(R.string.chamada_observacao)) },
                placeholder = { Text(stringResource(R.string.chamada_observacao_exemplo)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focoNoCampo),
            )

            SugestoesDeObservacao(aoEscolher = { sugestao -> rascunho.value = sugestao })

            Row(modifier = Modifier.fillMaxWidth()) {
                if (jaTinhaObservacao) {
                    TextButton(onClick = { aoConcluir("") }) {
                        Text(stringResource(R.string.chamada_remover_observacao))
                    }
                }
                Spacer(Modifier.weight(1f))
                Button(onClick = { aoConcluir(rascunho.value) }) {
                    Text(stringResource(R.string.concluir))
                }
            }
        }
    }
}

/** Atalhos para os motivos mais comuns: um toque preenche o campo. */
@Composable
private fun SugestoesDeObservacao(aoEscolher: (sugestao: String) -> Unit) {
    val atestado: String = stringResource(R.string.chamada_sugestao_atestado)
    val familiaAvisou: String = stringResource(R.string.chamada_sugestao_familia_avisou)

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SuggestionChip(
            onClick = { aoEscolher(atestado) },
            label = { Text(atestado) },
        )
        SuggestionChip(
            onClick = { aoEscolher(familiaAvisou) },
            label = { Text(familiaAvisou) },
        )
    }
}
