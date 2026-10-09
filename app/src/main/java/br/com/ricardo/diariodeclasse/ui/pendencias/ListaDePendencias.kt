package br.com.ricardo.diariodeclasse.ui.pendencias

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.ricardo.diariodeclasse.R
import br.com.ricardo.diariodeclasse.data.local.entity.Aluno
import br.com.ricardo.diariodeclasse.data.local.entity.Pendencia
import br.com.ricardo.diariodeclasse.ui.componentes.BotaoFlutuante
import br.com.ricardo.diariodeclasse.ui.componentes.MensagemCentralizada
import br.com.ricardo.diariodeclasse.ui.componentes.SeletorDeTurma
import br.com.ricardo.diariodeclasse.ui.componentes.TelaCarregando
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Estado do painel de pendência: fechado, criando (com ou sem aluno já escolhido) ou editando. */
private sealed interface PainelDePendencia {
    data object Fechado : PainelDePendencia
    data class Criando(val alunoInicial: Aluno?) : PainelDePendencia
    data class Editando(val pendencia: Pendencia, val aluno: Aluno) : PainelDePendencia
}

/**
 * Seção "Alunos" da aba "A fazer": as pendências da turma ativa, agrupadas por aluno.
 * A barra do topo é da aba; aqui ficam a lista, o botão de nova pendência e os avisos.
 */
@Composable
fun ListaDePendencias(
    aoAbrirAluno: (alunoId: String) -> Unit,
    viewModel: PendenciasViewModel = hiltViewModel(),
) {
    val estado: PendenciasUiState = viewModel.uiState.collectAsStateWithLifecycle().value

    when (estado) {
        is PendenciasUiState.Carregando -> TelaCarregando()
        is PendenciasUiState.NenhumaTurma -> MensagemCentralizada(stringResource(R.string.pendencias_sem_turma))
        is PendenciasUiState.Carregado -> ConteudoPendencias(estado, viewModel, aoAbrirAluno)
    }
}

@Composable
private fun ConteudoPendencias(
    estado: PendenciasUiState.Carregado,
    viewModel: PendenciasViewModel,
    aoAbrirAluno: (alunoId: String) -> Unit,
) {
    val painel: MutableState<PainelDePendencia> = remember { mutableStateOf(PainelDePendencia.Fechado) }
    val avisos: SnackbarHostState = remember { SnackbarHostState() }
    // Escopo de coroutine preso à tela: `showSnackbar` é `suspend` (espera o aviso sumir).
    val escopo: CoroutineScope = rememberCoroutineScope()
    val textoPendenciaEntregue: String = stringResource(R.string.pendencia_entregue_aviso)
    val textoPendenciaExcluida: String = stringResource(R.string.pendencia_excluida_aviso)
    val textoDesfazer: String = stringResource(R.string.pendencia_desfazer)
    val temAlunos: Boolean = estado.alunos.isNotEmpty()

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

    fun marcarComoEntregue(pendencia: Pendencia) {
        viewModel.marcarComoEntregue(pendencia.id)
        avisarComDesfazer(textoPendenciaEntregue, aoDesfazer = { viewModel.desfazerEntrega(pendencia.id) })
    }

    fun excluirPendencia(pendencia: Pendencia) {
        viewModel.excluirPendencia(pendencia.id)
        avisarComDesfazer(textoPendenciaExcluida, aoDesfazer = { viewModel.restaurarPendencia(pendencia.id) })
    }

    Scaffold(
        snackbarHost = { SnackbarHost(avisos) },
        floatingActionButton = {
            if (temAlunos) {
                BotaoFlutuante(
                    texto = stringResource(R.string.pendencias_nova),
                    aoClicar = { painel.value = PainelDePendencia.Criando(alunoInicial = null) },
                )
            }
        },
    ) { espacamentoDasBarras ->
        // O cabeçalho (com o seletor de turma) aparece também nos estados vazios:
        // é por ele que a professora troca para uma turma que tenha pendências.
        Column(
            modifier = Modifier
                .padding(espacamentoDasBarras)
                .fillMaxSize(),
        ) {
            CabecalhoDasPendencias(
                estado = estado,
                aoSelecionarTurma = { turmaId -> viewModel.selecionarTurma(turmaId) },
            )
            HorizontalDivider()

            if (!temAlunos) {
                MensagemCentralizada(stringResource(R.string.pendencias_sem_alunos))
            } else if (estado.grupos.isEmpty()) {
                MensagemCentralizada(stringResource(R.string.pendencias_vazio))
            } else {
                // O espaço extra no fim evita que o botão flutuante cubra a última pendência.
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 88.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    for (grupo in estado.grupos) {
                        item(key = grupo.aluno.id) {
                            CabecalhoDoAluno(
                                grupo = grupo,
                                aoAbrirAluno = { aoAbrirAluno(grupo.aluno.id) },
                                aoAdicionar = { painel.value = PainelDePendencia.Criando(alunoInicial = grupo.aluno) },
                            )
                        }
                        items(grupo.pendencias, key = { item -> item.pendencia.id }) { item ->
                            LinhaDaPendencia(
                                item = item,
                                hoje = estado.hoje,
                                aoEditar = { painel.value = PainelDePendencia.Editando(item.pendencia, grupo.aluno) },
                                aoMarcarComoEntregue = { marcarComoEntregue(item.pendencia) },
                            )
                        }
                        item {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }
    }

    val fecharPainel = { painel.value = PainelDePendencia.Fechado }

    when (val painelAtual: PainelDePendencia = painel.value) {
        is PainelDePendencia.Fechado -> {}

        is PainelDePendencia.Criando -> FolhaPendencia(
            titulo = stringResource(R.string.pendencias_nova),
            alunos = estado.alunos,
            alunoInicial = painelAtual.alunoInicial,
            podeTrocarAluno = true,
            descricaoInicial = "",
            dataInicial = estado.hoje,
            hoje = estado.hoje,
            aoSalvar = { alunoId, descricao, dataLembrete ->
                viewModel.criarPendencia(alunoId, descricao, dataLembrete)
                fecharPainel()
            },
            aoExcluir = null,
            aoFechar = fecharPainel,
        )

        is PainelDePendencia.Editando -> FolhaPendencia(
            titulo = stringResource(R.string.pendencia_editar),
            alunos = estado.alunos,
            alunoInicial = painelAtual.aluno,
            podeTrocarAluno = false,
            descricaoInicial = painelAtual.pendencia.descricao,
            dataInicial = painelAtual.pendencia.dataLembrete,
            hoje = estado.hoje,
            aoSalvar = { _, descricao, dataLembrete ->
                viewModel.editarPendencia(painelAtual.pendencia.id, descricao, dataLembrete)
                fecharPainel()
            },
            aoExcluir = {
                excluirPendencia(painelAtual.pendencia)
                fecharPainel()
            },
            aoFechar = fecharPainel,
        )
    }
}

/** Seletor da turma e, havendo pendências, o resumo: "5 pendências · 3 para hoje". */
@Composable
private fun CabecalhoDasPendencias(
    estado: PendenciasUiState.Carregado,
    aoSelecionarTurma: (turmaId: String) -> Unit,
) {
    val total: Int = estado.quantidadeTotal()
    val paraHoje: Int = estado.quantidadeParaHoje()
    val resumo = pluralStringResource(R.plurals.pendencias_total, total, total) +
        " · " + stringResource(R.string.pendencias_para_hoje, paraHoje)

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        SeletorDeTurma(
            turmaAtiva = estado.turma,
            todasAsTurmas = estado.todasAsTurmas,
            aoSelecionarTurma = aoSelecionarTurma,
        )
        // Sem pendências, a mensagem abaixo já diz isso; "0 pendências" só repetiria.
        if (total > 0) {
            Text(
                text = resumo,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CabecalhoDoAluno(
    grupo: GrupoDePendencias,
    aoAbrirAluno: () -> Unit,
    aoAdicionar: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(end = 4.dp, top = 8.dp),
    ) {
        NomeQueAbreOAluno(nome = grupo.aluno.nome, aoTocar = aoAbrirAluno, modifier = Modifier.weight(1f))
        IconButton(onClick = aoAdicionar) {
            Icon(
                painter = painterResource(R.drawable.ic_adicionar),
                contentDescription = stringResource(R.string.pendencia_adicionar_para, grupo.aluno.nome),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * O nome do aluno abre a página dele. A seta ao lado indica que o nome leva a
 * outra tela; a área de toque tem pelo menos 48dp de altura.
 */
@Composable
private fun NomeQueAbreOAluno(nome: String, aoTocar: () -> Unit, modifier: Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .heightIn(min = 48.dp)
            .clickable(onClick = aoTocar)
            .padding(start = 16.dp),
    ) {
        Text(
            text = nome,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.secondary,
        )
        Icon(
            painter = painterResource(R.drawable.ic_abrir),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(18.dp),
        )
    }
}
