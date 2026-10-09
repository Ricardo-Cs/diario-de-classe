package br.com.ricardo.diariodeclasse.ui.turmas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.ricardo.diariodeclasse.R
import br.com.ricardo.diariodeclasse.data.local.entity.Aluno
import br.com.ricardo.diariodeclasse.ui.alunos.FolhaAdicionarAlunos
import br.com.ricardo.diariodeclasse.ui.componentes.BarraSuperior
import br.com.ricardo.diariodeclasse.ui.componentes.BotaoFlutuante
import br.com.ricardo.diariodeclasse.ui.componentes.MensagemCentralizada
import br.com.ricardo.diariodeclasse.ui.componentes.SeletorDeTurma
import br.com.ricardo.diariodeclasse.ui.componentes.TelaCarregando

/**
 * Aba Turma: os alunos da turma ativa. As outras turmas ficam no seletor do
 * topo, e "Nova turma" fica ao lado dele, porque cadastrar turma é raro
 * (começo do ano) mas precisa ser fácil de achar.
 */
@Composable
fun TurmaScreen(
    aoCriarTurma: () -> Unit,
    aoEditarTurma: (turmaId: String) -> Unit,
    aoAbrirAluno: (alunoId: String) -> Unit,
    viewModel: TurmaViewModel = hiltViewModel(),
) {
    val estado: TurmaUiState = viewModel.uiState.collectAsStateWithLifecycle().value

    when (estado) {
        is TurmaUiState.Carregando -> TelaCarregando()
        is TurmaUiState.NenhumaTurma -> SemTurmas(aoCriarTurma = aoCriarTurma)
        is TurmaUiState.Carregado -> ConteudoTurma(
            estado = estado,
            viewModel = viewModel,
            aoCriarTurma = aoCriarTurma,
            aoEditarTurma = aoEditarTurma,
            aoAbrirAluno = aoAbrirAluno,
        )
    }
}

@Composable
private fun ConteudoTurma(
    estado: TurmaUiState.Carregado,
    viewModel: TurmaViewModel,
    aoCriarTurma: () -> Unit,
    aoEditarTurma: (turmaId: String) -> Unit,
    aoAbrirAluno: (alunoId: String) -> Unit,
) {
    val adicionandoAlunos: MutableState<Boolean> = remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            BarraSuperior(
                titulo = stringResource(R.string.aba_turma),
                acoes = {
                    // Excluir a turma fica dentro da edição, longe do toque acidental.
                    IconButton(onClick = { aoEditarTurma(estado.turma.id) }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_editar),
                            contentDescription = stringResource(R.string.turma_editar_descricao, estado.turma.nome),
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
        Column(
            modifier = Modifier
                .padding(espacamentoDasBarras)
                .fillMaxSize(),
        ) {
            CabecalhoDaTurma(
                estado = estado,
                aoSelecionarTurma = { turmaId -> viewModel.selecionarTurma(turmaId) },
                aoCriarTurma = aoCriarTurma,
            )
            HorizontalDivider()

            if (estado.alunos.isEmpty()) {
                MensagemCentralizada(stringResource(R.string.turma_alunos_vazio))
            } else {
                ListaDeAlunos(
                    alunos = estado.alunos,
                    aoClicarAluno = { aluno -> aoAbrirAluno(aluno.id) },
                )
            }
        }
    }

    if (adicionandoAlunos.value) {
        FolhaAdicionarAlunos(
            aoAdicionar = { nome -> viewModel.adicionarAluno(estado.turma.id, nome) },
            aoConcluir = { adicionandoAlunos.value = false },
        )
    }
}

@Composable
private fun CabecalhoDaTurma(
    estado: TurmaUiState.Carregado,
    aoSelecionarTurma: (turmaId: String) -> Unit,
    aoCriarTurma: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
    ) {
        SeletorDeTurma(
            turmaAtiva = estado.turma,
            todasAsTurmas = estado.todasAsTurmas,
            aoSelecionarTurma = aoSelecionarTurma,
        )
        Spacer(Modifier.weight(1f))
        TextButton(onClick = aoCriarTurma) {
            Text(stringResource(R.string.turmas_nova))
        }
    }
}

@Composable
private fun ListaDeAlunos(
    alunos: List<Aluno>,
    aoClicarAluno: (Aluno) -> Unit,
) {
    // O espaço extra no fim evita que o botão flutuante cubra o último aluno.
    LazyColumn(
        contentPadding = PaddingValues(bottom = 88.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(alunos, key = { aluno -> aluno.id }) { aluno ->
            ListItem(
                headlineContent = { Text(aluno.nome) },
                modifier = Modifier.clickable(onClick = { aoClicarAluno(aluno) }),
            )
        }
    }
}

/** Antes da primeira turma não há aluno para listar: só o convite para cadastrar. */
@Composable
private fun SemTurmas(aoCriarTurma: () -> Unit) {
    Scaffold(
        topBar = { BarraSuperior(titulo = stringResource(R.string.aba_turma)) },
    ) { espacamentoDasBarras ->
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .padding(espacamentoDasBarras)
                .padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.inicio_sem_turmas),
                style = MaterialTheme.typography.bodyLarge,
            )
            Button(onClick = aoCriarTurma) {
                Text(stringResource(R.string.inicio_cadastrar_turma))
            }
        }
    }
}
