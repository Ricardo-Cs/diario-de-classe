package br.com.ricardo.diariodeclasse.ui.alunos

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import br.com.ricardo.diariodeclasse.data.local.entity.Pendencia
import br.com.ricardo.diariodeclasse.ui.componentes.BarraSuperior
import br.com.ricardo.diariodeclasse.ui.componentes.BotaoFlutuante
import br.com.ricardo.diariodeclasse.ui.componentes.DialogoConfirmacao
import br.com.ricardo.diariodeclasse.ui.componentes.TelaCarregando
import br.com.ricardo.diariodeclasse.ui.pendencias.FolhaPendencia
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** O que está aberto por cima da tela (no máximo uma coisa por vez). */
private sealed interface Sobreposicao {
    data object Nenhuma : Sobreposicao
    data object NovaAnotacao : Sobreposicao
    data class EditandoAnotacao(val anotacao: Anotacao) : Sobreposicao
    data object NovaPendencia : Sobreposicao
    data class EditandoPendencia(val pendencia: Pendencia) : Sobreposicao
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
    val textoAnotacaoExcluida: String = stringResource(R.string.anotacao_excluida_aviso)
    val textoPendenciaEntregue: String = stringResource(R.string.pendencia_entregue_aviso)
    val textoPendenciaExcluida: String = stringResource(R.string.pendencia_excluida_aviso)
    val textoDesfazer: String = stringResource(R.string.pendencia_desfazer)

    /** Aviso no rodapé com "Desfazer", para o caso de um toque por engano. */
    fun avisarComDesfazer(mensagem: String, aoDesfazer: () -> Unit) {
        escopo.launch {
            avisos.currentSnackbarData?.dismiss()
            val resultado: SnackbarResult = avisos.showSnackbar(
                message = mensagem,
                actionLabel = textoDesfazer,
                duration = SnackbarDuration.Short,
            )
            if (resultado == SnackbarResult.ActionPerformed) {
                aoDesfazer()
            }
        }
    }

    fun excluirAnotacao(anotacao: Anotacao) {
        viewModel.excluirAnotacao(anotacao.id)
        avisarComDesfazer(textoAnotacaoExcluida, aoDesfazer = { viewModel.restaurarAnotacao(anotacao.id) })
    }

    fun marcarComoEntregue(pendencia: Pendencia) {
        viewModel.marcarPendenciaComoEntregue(pendencia.id)
        avisarComDesfazer(textoPendenciaEntregue, aoDesfazer = { viewModel.desfazerEntregaDaPendencia(pendencia.id) })
    }

    fun excluirPendencia(pendencia: Pendencia) {
        viewModel.excluirPendencia(pendencia.id)
        avisarComDesfazer(textoPendenciaExcluida, aoDesfazer = { viewModel.restaurarPendencia(pendencia.id) })
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
            BotaoFlutuante(
                texto = stringResource(R.string.anotacao_nova),
                aoClicar = { sobreposicao.value = Sobreposicao.NovaAnotacao },
            )
        },
    ) { espacamentoDasBarras ->
        // O espaço extra no fim evita que o botão flutuante cubra a última anotação.
        LazyColumn(
            contentPadding = PaddingValues(bottom = 88.dp),
            modifier = Modifier
                .padding(espacamentoDasBarras)
                .fillMaxSize(),
        ) {
            secaoDePendencias(
                aluno = estado.aluno,
                pendencias = estado.pendencias,
                hoje = estado.hoje,
                aoAdicionar = { sobreposicao.value = Sobreposicao.NovaPendencia },
                aoEditar = { pendencia -> sobreposicao.value = Sobreposicao.EditandoPendencia(pendencia) },
                aoMarcarComoEntregue = { pendencia -> marcarComoEntregue(pendencia) },
            )
            secaoDeFaltas(resumo = estado.faltas, hoje = estado.hoje)
            secaoDeAcompanhamento(evolucoes = estado.evolucoes, hoje = estado.hoje)
            secaoDeAnotacoes(
                aluno = estado.aluno,
                anotacoes = estado.anotacoes,
                hoje = estado.hoje,
                aoTocar = { anotacao -> sobreposicao.value = Sobreposicao.EditandoAnotacao(anotacao) },
            )
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

        is Sobreposicao.NovaPendencia -> FolhaPendencia(
            titulo = stringResource(R.string.pendencias_nova),
            alunos = listOf(estado.aluno),
            alunoInicial = estado.aluno,
            podeTrocarAluno = false,
            descricaoInicial = "",
            dataInicial = estado.hoje,
            hoje = estado.hoje,
            aoSalvar = { _, descricao, dataLembrete ->
                viewModel.criarPendencia(descricao, dataLembrete)
                fechar()
            },
            aoExcluir = null,
            aoFechar = fechar,
        )

        is Sobreposicao.EditandoPendencia -> FolhaPendencia(
            titulo = stringResource(R.string.pendencia_editar),
            alunos = listOf(estado.aluno),
            alunoInicial = estado.aluno,
            podeTrocarAluno = false,
            descricaoInicial = aberta.pendencia.descricao,
            dataInicial = aberta.pendencia.dataLembrete,
            hoje = estado.hoje,
            aoSalvar = { _, descricao, dataLembrete ->
                viewModel.editarPendencia(aberta.pendencia.id, descricao, dataLembrete)
                fechar()
            },
            aoExcluir = {
                excluirPendencia(aberta.pendencia)
                fechar()
            },
            aoFechar = fechar,
        )

        is Sobreposicao.EditandoAluno -> FolhaEditarAluno(
            aluno = estado.aluno,
            aoSalvar = { novoNome ->
                viewModel.renomearAluno(estado.aluno, novoNome)
                fechar()
            },
            aoExcluir = { sobreposicao.value = Sobreposicao.ConfirmandoExclusaoDoAluno },
            aoFechar = fechar,
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
