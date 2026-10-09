package br.com.ricardo.diariodeclasse.ui.lembretes

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.ricardo.diariodeclasse.R
import br.com.ricardo.diariodeclasse.data.local.entity.Lembrete
import br.com.ricardo.diariodeclasse.ui.componentes.BotaoFlutuante
import br.com.ricardo.diariodeclasse.ui.componentes.TelaCarregando
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Painel de lembrete: fechado, criando ou editando um lembrete. */
private sealed interface PainelDeLembrete {
    data object Fechado : PainelDeLembrete
    data object Criando : PainelDeLembrete
    data class Editando(val lembrete: Lembrete) : PainelDeLembrete
}

/**
 * Seção "Meus lembretes" da aba "A fazer". A barra do topo é da aba; aqui ficam
 * a lista, o botão de novo lembrete e os avisos.
 */
@Composable
fun ListaDeLembretes(viewModel: LembretesViewModel = hiltViewModel()) {
    val estado: LembretesUiState = viewModel.uiState.collectAsStateWithLifecycle().value

    when (estado) {
        is LembretesUiState.Carregando -> TelaCarregando()
        is LembretesUiState.Carregado -> ConteudoLembretes(estado, viewModel)
    }
}

@Composable
private fun ConteudoLembretes(
    estado: LembretesUiState.Carregado,
    viewModel: LembretesViewModel,
) {
    val painel: MutableState<PainelDeLembrete> = remember { mutableStateOf(PainelDeLembrete.Fechado) }
    val avisos: SnackbarHostState = remember { SnackbarHostState() }
    val escopo: CoroutineScope = rememberCoroutineScope()
    val textoConcluido: String = stringResource(R.string.lembrete_concluido_aviso)
    val textoExcluido: String = stringResource(R.string.lembrete_excluido_aviso)
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

    /** Em aberto: conclui (com "Desfazer"). Concluído: reabre direto, sem aviso. */
    fun alternarConclusao(lembrete: Lembrete) {
        if (lembrete.concluidoEm == null) {
            viewModel.marcarComoConcluido(lembrete.id)
            avisarComDesfazer(textoConcluido, aoDesfazer = { viewModel.reabrir(lembrete.id) })
        } else {
            viewModel.reabrir(lembrete.id)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(avisos) },
        floatingActionButton = {
            BotaoFlutuante(
                texto = stringResource(R.string.lembrete_novo),
                aoClicar = { painel.value = PainelDeLembrete.Criando },
            )
        },
    ) { espacamentoDasBarras ->
        // O espaço extra no fim evita que o botão flutuante cubra o último lembrete.
        LazyColumn(
            contentPadding = PaddingValues(bottom = 88.dp),
            modifier = Modifier
                .padding(espacamentoDasBarras)
                .fillMaxSize(),
        ) {
            if (estado.emAberto.isEmpty()) {
                item(key = "vazio") {
                    TextoDiscreto(stringResource(R.string.lembretes_vazio))
                }
            }
            items(estado.emAberto, key = { lembrete -> lembrete.id }) { lembrete ->
                LinhaDoLembrete(
                    lembrete = lembrete,
                    hoje = estado.hoje,
                    aoEditar = { painel.value = PainelDeLembrete.Editando(lembrete) },
                    aoAlternarConclusao = { alternarConclusao(lembrete) },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }

            if (estado.concluidos.isNotEmpty()) {
                item(key = "cabecalho_concluidos") {
                    Text(
                        text = stringResource(R.string.lembretes_concluidos),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .padding(start = 16.dp, top = 20.dp),
                    )
                }
                items(estado.concluidos, key = { lembrete -> lembrete.id }) { lembrete ->
                    LinhaDoLembrete(
                        lembrete = lembrete,
                        hoje = estado.hoje,
                        aoEditar = { painel.value = PainelDeLembrete.Editando(lembrete) },
                        aoAlternarConclusao = { alternarConclusao(lembrete) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }

    val fecharPainel = { painel.value = PainelDeLembrete.Fechado }

    when (val painelAtual: PainelDeLembrete = painel.value) {
        is PainelDeLembrete.Fechado -> {}

        is PainelDeLembrete.Criando -> FolhaLembrete(
            titulo = stringResource(R.string.lembrete_novo),
            descricaoInicial = "",
            dataInicial = null,
            hoje = estado.hoje,
            aoSalvar = { descricao, data ->
                viewModel.criar(descricao, data)
                fecharPainel()
            },
            aoExcluir = null,
            aoFechar = fecharPainel,
        )

        is PainelDeLembrete.Editando -> FolhaLembrete(
            titulo = stringResource(R.string.lembrete_editar),
            descricaoInicial = painelAtual.lembrete.descricao,
            dataInicial = painelAtual.lembrete.data,
            hoje = estado.hoje,
            aoSalvar = { descricao, data ->
                viewModel.editar(painelAtual.lembrete.id, descricao, data)
                fecharPainel()
            },
            aoExcluir = {
                viewModel.excluir(painelAtual.lembrete.id)
                avisarComDesfazer(textoExcluido, aoDesfazer = { viewModel.restaurar(painelAtual.lembrete.id) })
                fecharPainel()
            },
            aoFechar = fecharPainel,
        )
    }
}

@Composable
private fun TextoDiscreto(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(16.dp),
    )
}
