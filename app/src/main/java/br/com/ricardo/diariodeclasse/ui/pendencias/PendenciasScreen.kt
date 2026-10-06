package br.com.ricardo.diariodeclasse.ui.pendencias

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.ricardo.diariodeclasse.R
import br.com.ricardo.diariodeclasse.data.local.entity.Aluno
import br.com.ricardo.diariodeclasse.data.local.entity.Pendencia
import br.com.ricardo.diariodeclasse.ui.componentes.BarraSuperior
import br.com.ricardo.diariodeclasse.ui.componentes.MensagemCentralizada
import br.com.ricardo.diariodeclasse.ui.componentes.TelaCarregando
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Estado do painel de nova pendência: fechado, ou aberto (com ou sem aluno já escolhido). */
private sealed interface PainelNovaPendencia {
    data object Fechado : PainelNovaPendencia
    data class Aberto(val alunoInicial: Aluno?) : PainelNovaPendencia
}

@Composable
fun PendenciasScreen(
    aoVoltar: () -> Unit,
    viewModel: PendenciasViewModel = hiltViewModel(),
) {
    val estado: PendenciasUiState = viewModel.uiState.collectAsStateWithLifecycle().value

    when (estado) {
        is PendenciasUiState.Carregando -> TelaCarregando()

        is PendenciasUiState.TurmaNaoEncontrada -> {
            LaunchedEffect(Unit) {
                aoVoltar()
            }
        }

        is PendenciasUiState.Carregado -> ConteudoPendencias(estado, viewModel, aoVoltar)
    }
}

@Composable
private fun ConteudoPendencias(
    estado: PendenciasUiState.Carregado,
    viewModel: PendenciasViewModel,
    aoVoltar: () -> Unit,
) {
    val painel: MutableState<PainelNovaPendencia> = remember { mutableStateOf(PainelNovaPendencia.Fechado) }
    val avisos: SnackbarHostState = remember { SnackbarHostState() }
    // Escopo de coroutine preso à tela: `showSnackbar` é `suspend` (espera o aviso sumir).
    val escopo: CoroutineScope = rememberCoroutineScope()
    val textoDoAviso: String = stringResource(R.string.pendencia_entregue_aviso)
    val textoDesfazer: String = stringResource(R.string.pendencia_desfazer)
    val temAlunos: Boolean = estado.alunos.isNotEmpty()

    /** Marca e mostra o aviso com "Desfazer" para o caso de um toque por engano. */
    fun marcarComoEntregue(pendencia: Pendencia) {
        viewModel.marcarComoEntregue(pendencia.id)
        escopo.launch {
            avisos.currentSnackbarData?.dismiss()
            val resultado: SnackbarResult = avisos.showSnackbar(
                message = textoDoAviso,
                actionLabel = textoDesfazer,
                duration = SnackbarDuration.Short,
            )
            if (resultado == SnackbarResult.ActionPerformed) {
                viewModel.desfazerEntrega(pendencia.id)
            }
        }
    }

    Scaffold(
        topBar = { BarraSuperior(titulo = stringResource(R.string.pendencias_titulo), aoVoltar = aoVoltar) },
        snackbarHost = { SnackbarHost(avisos) },
        floatingActionButton = {
            if (temAlunos) {
                ExtendedFloatingActionButton(onClick = { painel.value = PainelNovaPendencia.Aberto(alunoInicial = null) }) {
                    Text(stringResource(R.string.pendencias_nova))
                }
            }
        },
    ) { espacamentoDasBarras ->
        val modifier = Modifier.padding(espacamentoDasBarras)

        if (!temAlunos) {
            MensagemCentralizada(stringResource(R.string.pendencias_sem_alunos), modifier)
        } else if (estado.grupos.isEmpty()) {
            MensagemCentralizada(stringResource(R.string.pendencias_vazio), modifier)
        } else {
            LazyColumn(modifier = modifier.fillMaxSize()) {
                item {
                    CabecalhoDasPendencias(estado)
                    HorizontalDivider()
                }
                for (grupo in estado.grupos) {
                    item(key = grupo.aluno.id) {
                        CabecalhoDoAluno(
                            grupo = grupo,
                            aoAdicionar = { painel.value = PainelNovaPendencia.Aberto(alunoInicial = grupo.aluno) },
                        )
                    }
                    items(grupo.pendencias, key = { pendencia -> pendencia.id }) { pendencia ->
                        LinhaDaPendencia(
                            pendencia = pendencia,
                            hoje = estado.hoje,
                            aoMarcarComoEntregue = { marcarComoEntregue(pendencia) },
                        )
                    }
                    item {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }

    val painelAtual: PainelNovaPendencia = painel.value
    if (painelAtual is PainelNovaPendencia.Aberto) {
        FolhaNovaPendencia(
            alunos = estado.alunos,
            alunoInicial = painelAtual.alunoInicial,
            hoje = estado.hoje,
            aoSalvar = { alunoId, descricao, dataLembrete ->
                viewModel.criarPendencia(alunoId, descricao, dataLembrete)
                painel.value = PainelNovaPendencia.Fechado
            },
            aoFechar = { painel.value = PainelNovaPendencia.Fechado },
        )
    }
}

/** Turma e resumo: "5 pendências · 3 para hoje". */
@Composable
private fun CabecalhoDasPendencias(estado: PendenciasUiState.Carregado) {
    val total: Int = estado.quantidadeTotal()
    val paraHoje: Int = estado.quantidadeParaHoje()
    val resumo = pluralStringResource(R.plurals.pendencias_total, total, total) +
        " · " + stringResource(R.string.pendencias_para_hoje, paraHoje)

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(text = estado.turma.nome, style = MaterialTheme.typography.titleMedium)
        Text(
            text = resumo,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CabecalhoDoAluno(grupo: GrupoDePendencias, aoAdicionar: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 8.dp),
    ) {
        Text(
            text = grupo.aluno.nome,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = aoAdicionar) {
            Icon(
                painter = painterResource(R.drawable.ic_adicionar),
                contentDescription = stringResource(R.string.pendencia_adicionar_para, grupo.aluno.nome),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LinhaDaPendencia(
    pendencia: Pendencia,
    hoje: LocalDate,
    aoMarcarComoEntregue: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 16.dp, end = 4.dp, bottom = 4.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = pendencia.descricao, style = MaterialTheme.typography.bodyLarge)
            TextoDoLembrete(dataLembrete = pendencia.dataLembrete, hoje = hoje)
        }
        IconButton(onClick = aoMarcarComoEntregue) {
            Icon(
                painter = painterResource(R.drawable.ic_presente),
                contentDescription = stringResource(R.string.pendencia_marcar_entregue),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * - antes de hoje: "Pendente desde 05/10" (vermelho: já devia ter sido resolvida);
 * - hoje: "Para hoje" (cor de destaque);
 * - amanhã ou depois: "Para amanhã" / "Para 09/10" (discreto).
 */
@Composable
private fun TextoDoLembrete(dataLembrete: LocalDate, hoje: LocalDate) {
    val dataCurta: String = dataLembrete.format(DateTimeFormatter.ofPattern("dd/MM"))
    val texto: String
    val cor: Color
    if (dataLembrete.isBefore(hoje)) {
        texto = stringResource(R.string.pendencia_lembrete_desde, dataCurta)
        cor = MaterialTheme.colorScheme.error
    } else if (dataLembrete == hoje) {
        texto = stringResource(R.string.pendencia_lembrete_hoje)
        cor = MaterialTheme.colorScheme.primary
    } else if (dataLembrete == hoje.plusDays(1)) {
        texto = stringResource(R.string.pendencia_lembrete_amanha)
        cor = MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        texto = stringResource(R.string.pendencia_lembrete_data, dataCurta)
        cor = MaterialTheme.colorScheme.onSurfaceVariant
    }

    Text(text = texto, style = MaterialTheme.typography.bodySmall, color = cor)
}
