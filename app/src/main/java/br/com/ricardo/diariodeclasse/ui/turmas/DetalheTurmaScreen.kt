package br.com.ricardo.diariodeclasse.ui.turmas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExtendedFloatingActionButton
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
import br.com.ricardo.diariodeclasse.ui.alunos.DialogoAdicionarAlunos
import br.com.ricardo.diariodeclasse.ui.alunos.DialogoEditarAluno
import br.com.ricardo.diariodeclasse.ui.componentes.BarraSuperior
import br.com.ricardo.diariodeclasse.ui.componentes.DialogoConfirmacao
import br.com.ricardo.diariodeclasse.ui.componentes.MensagemCentralizada
import br.com.ricardo.diariodeclasse.ui.componentes.TelaCarregando

/** Qual diálogo está aberto na tela (no máximo um por vez). */
private sealed interface Dialogo {
    data object Nenhum : Dialogo
    data object AdicionarAlunos : Dialogo
    data class EditarAluno(val aluno: Aluno) : Dialogo
    data class ConfirmarExclusaoAluno(val aluno: Aluno) : Dialogo
    data object ConfirmarExclusaoTurma : Dialogo
}

@Composable
fun DetalheTurmaScreen(
    aoEditarTurma: (turmaId: String) -> Unit,
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
    aoVoltar: () -> Unit,
) {
    val dialogoAberto: MutableState<Dialogo> = remember { mutableStateOf(Dialogo.Nenhum) }

    Scaffold(
        topBar = {
            BarraSuperior(
                titulo = turma.nome,
                aoVoltar = aoVoltar,
                acoes = {
                    IconButton(onClick = { aoEditarTurma(turma.id) }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_editar),
                            contentDescription = stringResource(R.string.editar),
                        )
                    }
                    IconButton(onClick = { dialogoAberto.value = Dialogo.ConfirmarExclusaoTurma }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_excluir),
                            contentDescription = stringResource(R.string.excluir),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { dialogoAberto.value = Dialogo.AdicionarAlunos }) {
                Text(stringResource(R.string.turma_adicionar_alunos))
            }
        },
    ) { espacamentoDasBarras ->
        val modifier = Modifier.padding(espacamentoDasBarras)

        if (alunos.isEmpty()) {
            MensagemCentralizada(stringResource(R.string.turma_alunos_vazio), modifier)
        } else {
            ListaDeAlunos(
                alunos = alunos,
                aoClicarAluno = { aluno -> dialogoAberto.value = Dialogo.EditarAluno(aluno) },
                modifier = modifier,
            )
        }
    }

    val fecharDialogo = { dialogoAberto.value = Dialogo.Nenhum }
    val dialogo: Dialogo = dialogoAberto.value

    when (dialogo) {
        is Dialogo.Nenhum -> {}

        is Dialogo.AdicionarAlunos -> DialogoAdicionarAlunos(
            aoAdicionar = { nome -> viewModel.adicionarAluno(nome) },
            aoConcluir = fecharDialogo,
        )

        is Dialogo.EditarAluno -> DialogoEditarAluno(
            aluno = dialogo.aluno,
            aoSalvar = { novoNome ->
                viewModel.renomearAluno(dialogo.aluno, novoNome)
                fecharDialogo()
            },
            aoExcluir = { dialogoAberto.value = Dialogo.ConfirmarExclusaoAluno(dialogo.aluno) },
            aoCancelar = fecharDialogo,
        )

        is Dialogo.ConfirmarExclusaoAluno -> DialogoConfirmacao(
            titulo = stringResource(R.string.aluno_excluir_titulo),
            mensagem = stringResource(R.string.aluno_excluir_mensagem, dialogo.aluno.nome),
            textoConfirmar = stringResource(R.string.excluir),
            aoConfirmar = {
                viewModel.excluirAluno(dialogo.aluno)
                fecharDialogo()
            },
            aoCancelar = fecharDialogo,
        )

        is Dialogo.ConfirmarExclusaoTurma -> DialogoConfirmacao(
            titulo = stringResource(R.string.turma_excluir_titulo),
            mensagem = stringResource(R.string.turma_excluir_mensagem, turma.nome),
            textoConfirmar = stringResource(R.string.excluir),
            aoConfirmar = {
                viewModel.excluirTurma()
                fecharDialogo()
            },
            aoCancelar = fecharDialogo,
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
