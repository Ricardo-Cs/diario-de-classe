package br.com.ricardo.diariodeclasse.ui.turmas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.ricardo.diariodeclasse.R
import br.com.ricardo.diariodeclasse.data.local.entity.Aluno
import br.com.ricardo.diariodeclasse.data.local.entity.Turma
import br.com.ricardo.diariodeclasse.ui.alunos.FolhaAdicionarAlunos
import br.com.ricardo.diariodeclasse.ui.componentes.BarraSuperior
import br.com.ricardo.diariodeclasse.ui.componentes.BotaoFlutuante
import br.com.ricardo.diariodeclasse.ui.componentes.MensagemCentralizada
import br.com.ricardo.diariodeclasse.ui.componentes.TelaCarregando

@Composable
fun DetalheTurmaScreen(
    aoEditarTurma: (turmaId: String) -> Unit,
    aoAbrirAluno: (alunoId: String) -> Unit,
    aoVoltar: () -> Unit,
    viewModel: DetalheTurmaViewModel = hiltViewModel(),
) {
    val estado: DetalheTurmaUiState = viewModel.uiState.collectAsStateWithLifecycle().value

    when (estado) {
        is DetalheTurmaUiState.Carregando -> TelaCarregando()

        is DetalheTurmaUiState.TurmaNaoEncontrada -> {
            LaunchedEffect(Unit) {
                aoVoltar()
            }
        }

        is DetalheTurmaUiState.Carregado -> ConteudoDetalheTurma(
            turma = estado.turma,
            alunos = estado.alunos,
            viewModel = viewModel,
            aoEditarTurma = aoEditarTurma,
            aoAbrirAluno = aoAbrirAluno,
            aoVoltar = aoVoltar,
        )
    }
}

@Composable
private fun ConteudoDetalheTurma(
    turma: Turma,
    alunos: List<Aluno>,
    viewModel: DetalheTurmaViewModel,
    aoEditarTurma: (turmaId: String) -> Unit,
    aoAbrirAluno: (alunoId: String) -> Unit,
    aoVoltar: () -> Unit,
) {
    val adicionandoAlunos: MutableState<Boolean> = remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            BarraSuperior(
                titulo = turma.nome,
                aoVoltar = aoVoltar,
                acoes = {
                    // Excluir a turma fica dentro da edição, longe do toque acidental.
                    IconButton(onClick = { aoEditarTurma(turma.id) }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_editar),
                            contentDescription = stringResource(R.string.editar),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            BotaoFlutuante(
                texto = stringResource(R.string.turma_adicionar_alunos),
                aoClicar = { adicionandoAlunos.value = true },
            )
        },
    ) { espacamentoDasBarras ->
        val modifier = Modifier.padding(espacamentoDasBarras)

        if (alunos.isEmpty()) {
            MensagemCentralizada(stringResource(R.string.turma_alunos_vazio), modifier)
        } else {
            ListaDeAlunos(
                alunos = alunos,
                aoClicarAluno = { aluno -> aoAbrirAluno(aluno.id) },
                modifier = modifier,
            )
        }
    }

    if (adicionandoAlunos.value) {
        FolhaAdicionarAlunos(
            aoAdicionar = { nome -> viewModel.adicionarAluno(nome) },
            aoConcluir = { adicionandoAlunos.value = false },
        )
    }
}

@Composable
private fun ListaDeAlunos(
    alunos: List<Aluno>,
    aoClicarAluno: (Aluno) -> Unit,
    modifier: Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(alunos, key = { aluno -> aluno.id }) { aluno ->
            ListItem(
                headlineContent = { Text(aluno.nome) },
                modifier = Modifier.clickable(onClick = { aoClicarAluno(aluno) }),
            )
        }
    }
}
