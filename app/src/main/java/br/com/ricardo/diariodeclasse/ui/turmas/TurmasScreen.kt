package br.com.ricardo.diariodeclasse.ui.turmas

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.ricardo.diariodeclasse.R
import br.com.ricardo.diariodeclasse.ui.theme.DiarioDeClasseTheme

/**
 * Composable "com estado": obtém o ViewModel e repassa só dados para [TurmasContent].
 * `collectAsStateWithLifecycle` para de coletar quando o app vai para segundo plano.
 */
@Composable
fun TurmasScreen(viewModel: TurmasViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TurmasContent(uiState)
}

/** Composable "sem estado": fácil de testar e de visualizar no Preview. */
@Composable
fun TurmasContent(uiState: TurmasUiState, modifier: Modifier = Modifier) {
    when (uiState) {
        TurmasUiState.Carregando -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        is TurmasUiState.Sucesso -> if (uiState.turmas.isEmpty()) {
            Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.turmas_vazio))
            }
        } else {
            LazyColumn(modifier.fillMaxSize()) {
                items(uiState.turmas, key = { it.id }) { turma ->
                    ListItem(
                        headlineContent = { Text(turma.nome) },
                        supportingContent = { Text("${turma.anoSerie} · ${turma.anoLetivo}") },
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TurmasVaziaPreview() {
    DiarioDeClasseTheme {
        TurmasContent(TurmasUiState.Sucesso(emptyList()))
    }
}
