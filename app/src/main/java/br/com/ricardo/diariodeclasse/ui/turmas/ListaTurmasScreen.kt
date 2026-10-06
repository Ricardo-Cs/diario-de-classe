package br.com.ricardo.diariodeclasse.ui.turmas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.ricardo.diariodeclasse.R
import br.com.ricardo.diariodeclasse.data.local.entity.Turma
import br.com.ricardo.diariodeclasse.ui.componentes.BarraSuperior
import br.com.ricardo.diariodeclasse.ui.componentes.BotaoFlutuante
import br.com.ricardo.diariodeclasse.ui.componentes.MensagemCentralizada
import br.com.ricardo.diariodeclasse.ui.componentes.TelaCarregando
import br.com.ricardo.diariodeclasse.ui.componentes.nomeDoPeriodo

@Composable
fun ListaTurmasScreen(
    aoAbrirTurma: (turmaId: String) -> Unit,
    aoCriarTurma: () -> Unit,
    viewModel: ListaTurmasViewModel = hiltViewModel(),
) {
    val estado: ListaTurmasUiState = viewModel.uiState.collectAsStateWithLifecycle().value

    Scaffold(
        topBar = { BarraSuperior(titulo = stringResource(R.string.turmas_titulo)) },
        floatingActionButton = {
            BotaoFlutuante(texto = stringResource(R.string.turmas_nova), aoClicar = aoCriarTurma)
        },
    ) { espacamentoDasBarras ->
        val modifier = Modifier.padding(espacamentoDasBarras)

        when (estado) {
            is ListaTurmasUiState.Carregando -> TelaCarregando(modifier)

            is ListaTurmasUiState.Carregado -> {
                if (estado.turmas.isEmpty()) {
                    MensagemCentralizada(stringResource(R.string.turmas_vazio), modifier)
                } else {
                    ListaDeTurmas(estado.turmas, aoAbrirTurma, modifier)
                }
            }
        }
    }
}

@Composable
private fun ListaDeTurmas(
    turmas: List<Turma>,
    aoAbrirTurma: (turmaId: String) -> Unit,
    modifier: Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(turmas, key = { turma -> turma.id }) { turma ->
            ItemTurma(turma = turma, aoClicar = { aoAbrirTurma(turma.id) })
        }
    }
}

@Composable
private fun ItemTurma(turma: Turma, aoClicar: () -> Unit) {
    val detalhes = "${turma.anoSerie} · ${nomeDoPeriodo(turma.periodo)} · ${turma.anoLetivo}"

    ListItem(
        headlineContent = { Text(turma.nome) },
        supportingContent = { Text(detalhes) },
        modifier = Modifier.clickable(onClick = aoClicar),
    )
}
