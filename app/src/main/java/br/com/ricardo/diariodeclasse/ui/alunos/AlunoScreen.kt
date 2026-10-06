package br.com.ricardo.diariodeclasse.ui.alunos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.ricardo.diariodeclasse.R
import br.com.ricardo.diariodeclasse.data.local.entity.Anotacao
import br.com.ricardo.diariodeclasse.ui.componentes.BarraSuperior
import br.com.ricardo.diariodeclasse.ui.componentes.DialogoConfirmacao
import br.com.ricardo.diariodeclasse.ui.componentes.MensagemCentralizada
import br.com.ricardo.diariodeclasse.ui.componentes.TelaCarregando
import br.com.ricardo.diariodeclasse.ui.componentes.textoDeDataRelativa
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate

/** O que está aberto por cima da tela (no máximo uma coisa por vez). */
private sealed interface Sobreposicao {
    data object Nenhuma : Sobreposicao
    data object NovaAnotacao : Sobreposicao
    data class EditandoAnotacao(val anotacao: Anotacao) : Sobreposicao
    data object EditandoAluno : Sobreposicao
    data object ConfirmandoExclusaoDoAluno : Sobreposicao
}

@Composable
fun AlunoScreen(
    aoVoltar: () -> Unit,
    viewModel: AlunoViewModel = hiltViewModel(),
) {
    val estado: AlunoUiState = viewModel.uiState.collectAsStateWithLifecycle().value

    when (estado) {
        is AlunoUiState.Carregando -> TelaCarregando()

        is AlunoUiState.AlunoNaoEncontrado -> {
            LaunchedEffect(Unit) {
                aoVoltar()
            }
        }

        is AlunoUiState.Carregado -> ConteudoAluno(estado, viewModel, aoVoltar)
    }
}

@Composable
private fun ConteudoAluno(
    estado: AlunoUiState.Carregado,
    viewModel: AlunoViewModel,
    aoVoltar: () -> Unit,
) {
    val sobreposicao: MutableState<Sobreposicao> = remember { mutableStateOf(Sobreposicao.Nenhuma) }
    val avisos: SnackbarHostState = remember { SnackbarHostState() }
    val escopo: CoroutineScope = rememberCoroutineScope()
    val textoDoAviso: String = stringResource(R.string.anotacao_excluida_aviso)
    val textoDesfazer: String = stringResource(R.string.pendencia_desfazer)

    /** Exclui e mostra o aviso com "Desfazer", como nas pendências. */
    fun excluirAnotacao(anotacao: Anotacao) {
        viewModel.excluirAnotacao(anotacao.id)
        escopo.launch {
            avisos.currentSnackbarData?.dismiss()
            val resultado: SnackbarResult = avisos.showSnackbar(
                message = textoDoAviso,
                actionLabel = textoDesfazer,
                duration = SnackbarDuration.Short,
            )
            if (resultado == SnackbarResult.ActionPerformed) {
                viewModel.restaurarAnotacao(anotacao.id)
            }
        }
    }

    Scaffold(
        topBar = {
            BarraSuperior(
                titulo = estado.aluno.nome,
                aoVoltar = aoVoltar,
                acoes = {
                    IconButton(onClick = { sobreposicao.value = Sobreposicao.EditandoAluno }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_editar),
                            contentDescription = stringResource(R.string.aluno_editar_titulo),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(avisos) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { sobreposicao.value = Sobreposicao.NovaAnotacao }) {
                Text(stringResource(R.string.anotacao_nova))
            }
        },
    ) { espacamentoDasBarras ->
        val modifier = Modifier.padding(espacamentoDasBarras)

        if (estado.anotacoes.isEmpty()) {
            MensagemCentralizada(stringResource(R.string.aluno_sem_anotacoes, estado.aluno.nome), modifier)
        } else {
            LazyColumn(modifier = modifier.fillMaxSize()) {
                item {
                    Text(
                        text = stringResource(R.string.aluno_anotacoes),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                    )
                }
                items(estado.anotacoes, key = { anotacao -> anotacao.id }) { anotacao ->
                    ItemAnotacao(
                        anotacao = anotacao,
                        hoje = estado.hoje,
                        aoTocar = { sobreposicao.value = Sobreposicao.EditandoAnotacao(anotacao) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }

    val fechar = { sobreposicao.value = Sobreposicao.Nenhuma }

    when (val aberta: Sobreposicao = sobreposicao.value) {
        is Sobreposicao.Nenhuma -> {}

        is Sobreposicao.NovaAnotacao -> FolhaAnotacao(
            titulo = stringResource(R.string.anotacao_nova),
            textoInicial = "",
            aoSalvar = { texto ->
                viewModel.criarAnotacao(texto)
                fechar()
            },
            aoExcluir = null,
            aoFechar = fechar,
        )

        is Sobreposicao.EditandoAnotacao -> FolhaAnotacao(
            titulo = stringResource(R.string.anotacao_editar),
            textoInicial = aberta.anotacao.texto,
            aoSalvar = { texto ->
                viewModel.editarAnotacao(aberta.anotacao.id, texto)
                fechar()
            },
            aoExcluir = {
                excluirAnotacao(aberta.anotacao)
                fechar()
            },
            aoFechar = fechar,
        )

        is Sobreposicao.EditandoAluno -> DialogoEditarAluno(
            aluno = estado.aluno,
            aoSalvar = { novoNome ->
                viewModel.renomearAluno(estado.aluno, novoNome)
                fechar()
            },
            aoExcluir = { sobreposicao.value = Sobreposicao.ConfirmandoExclusaoDoAluno },
            aoCancelar = fechar,
        )

        is Sobreposicao.ConfirmandoExclusaoDoAluno -> DialogoConfirmacao(
            titulo = stringResource(R.string.aluno_excluir_titulo),
            mensagem = stringResource(R.string.aluno_excluir_mensagem, estado.aluno.nome),
            textoConfirmar = stringResource(R.string.excluir),
            aoConfirmar = {
                viewModel.excluirAluno()
                fechar()
            },
            aoCancelar = fechar,
        )
    }
}

/** Data em destaque discreto ("Hoje", "Ontem", "02/10") e o texto completo abaixo. */
@Composable
private fun ItemAnotacao(anotacao: Anotacao, hoje: LocalDate, aoTocar: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = aoTocar)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = textoDeDataRelativa(anotacao.data, hoje),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = anotacao.texto, style = MaterialTheme.typography.bodyLarge)
    }
}
