package br.com.ricardo.diariodeclasse.ui.inicio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.ricardo.diariodeclasse.R
import br.com.ricardo.diariodeclasse.data.local.entity.Aluno
import br.com.ricardo.diariodeclasse.data.local.entity.Lembrete
import br.com.ricardo.diariodeclasse.ui.alunos.FolhaAnotacao
import br.com.ricardo.diariodeclasse.ui.componentes.BotaoFlutuante
import br.com.ricardo.diariodeclasse.ui.componentes.SeletorDeTurma
import br.com.ricardo.diariodeclasse.ui.componentes.formatarDataPorExtenso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Os passos do atalho "Anotar": fechado, escolhendo o aluno ou escrevendo sobre ele. */
private sealed interface AtalhoDeAnotacao {
    data object Fechado : AtalhoDeAnotacao
    data object EscolhendoAluno : AtalhoDeAnotacao
    data class Escrevendo(val aluno: Aluno) : AtalhoDeAnotacao
}

@Composable
fun InicioScreen(
    aoCadastrarTurma: () -> Unit,
    aoAbrirChamada: (turmaId: String, data: LocalDate) -> Unit,
    aoAbrirTurma: () -> Unit,
    aoAbrirPendencias: () -> Unit,
    aoAbrirLembretes: () -> Unit,
    aoAbrirConfiguracoes: () -> Unit,
    viewModel: InicioViewModel = hiltViewModel(),
) {
    val estado: InicioUiState = viewModel.uiState.collectAsStateWithLifecycle().value
    val avisos: SnackbarHostState = remember { SnackbarHostState() }
    val escopo: CoroutineScope = rememberCoroutineScope()
    val textoConcluido: String = stringResource(R.string.lembrete_concluido_aviso)
    val textoDesfazer: String = stringResource(R.string.pendencia_desfazer)
    val textoAnotacaoSalva: String = stringResource(R.string.inicio_anotacao_salva)
    val atalhoDeAnotacao: MutableState<AtalhoDeAnotacao> = remember { mutableStateOf(AtalhoDeAnotacao.Fechado) }

    // Roda toda vez que a tela volta ao primeiro plano (como o `onResume` de uma Activity).
    LifecycleResumeEffect(Unit) {
        viewModel.atualizarDataEHora()
        onPauseOrDispose { }
    }

    /** Conclui e oferece "Desfazer", para o caso de um toque por engano. */
    fun concluirLembrete(lembrete: Lembrete) {
        viewModel.marcarLembreteComoConcluido(lembrete.id)
        escopo.launch {
            avisos.currentSnackbarData?.dismiss()
            val resultado: SnackbarResult = avisos.showSnackbar(
                message = textoConcluido,
                actionLabel = textoDesfazer,
                duration = SnackbarDuration.Short,
            )
            if (resultado == SnackbarResult.ActionPerformed) {
                viewModel.reabrirLembrete(lembrete.id)
            }
        }
    }

    /** Salva e confirma no rodapé com o nome do aluno, já que a anotação não aparece no Início. */
    fun salvarAnotacao(aluno: Aluno, texto: String) {
        viewModel.criarAnotacao(aluno.id, texto)
        atalhoDeAnotacao.value = AtalhoDeAnotacao.Fechado
        escopo.launch {
            avisos.currentSnackbarData?.dismiss()
            avisos.showSnackbar(message = String.format(textoAnotacaoSalva, aluno.nome))
        }
    }

    // O atalho só faz sentido com alunos cadastrados na turma ativa.
    val turmasDoInicio: TurmasDoInicio = estado.turmas
    val podeAnotar: Boolean = turmasDoInicio is TurmasDoInicio.Carregadas && turmasDoInicio.alunos.isNotEmpty()

    Scaffold(
        snackbarHost = { SnackbarHost(avisos) },
        floatingActionButton = {
            if (podeAnotar) {
                BotaoFlutuante(
                    texto = stringResource(R.string.inicio_anotar),
                    aoClicar = { atalhoDeAnotacao.value = AtalhoDeAnotacao.EscolhendoAluno },
                )
            }
        },
    ) { espacamentoDasBarras ->
        Column(
            modifier = Modifier
                .padding(espacamentoDasBarras)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                // Embaixo, espaço para o botão "Anotar" não cobrir o último card.
                .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Cabecalho(
                saudacao = estado.saudacao,
                hoje = estado.hoje,
                aoAbrirConfiguracoes = aoAbrirConfiguracoes,
            )

            when (val turmas: TurmasDoInicio = estado.turmas) {
                is TurmasDoInicio.Carregando -> {
                    // Leitura local e rápida: um indicador de carregamento só piscaria na tela.
                }

                is TurmasDoInicio.NenhumaCadastrada -> {
                    AvisoSemTurmas(aoCadastrarTurma = aoCadastrarTurma)
                }

                is TurmasDoInicio.Carregadas -> {
                    SeletorDeTurma(
                        turmaAtiva = turmas.turmaAtiva,
                        todasAsTurmas = turmas.todas,
                        aoSelecionarTurma = { turmaId -> viewModel.selecionarTurma(turmaId) },
                    )
                    CardChamada(
                        situacao = turmas.chamadaDeHoje,
                        hoje = estado.hoje,
                        aoAbrirChamada = { aoAbrirChamada(turmas.turmaAtiva.id, estado.hoje) },
                        aoAbrirChamadaDeOutroDia = { data -> aoAbrirChamada(turmas.turmaAtiva.id, data) },
                        aoAdicionarAlunos = aoAbrirTurma,
                    )
                    CardPendencias(
                        resumo = turmas.pendencias,
                        aoAbrirPendencias = aoAbrirPendencias,
                    )
                }
            }

            // Fora do `when`: os lembretes são da professora e não dependem de turma.
            if (estado.turmas !is TurmasDoInicio.Carregando) {
                CardLembretes(
                    lembretes = estado.lembretes,
                    hoje = estado.hoje,
                    aoConcluir = { lembrete -> concluirLembrete(lembrete) },
                    aoAbrirLembretes = aoAbrirLembretes,
                )
            }
        }
    }

    when (val atalho: AtalhoDeAnotacao = atalhoDeAnotacao.value) {
        is AtalhoDeAnotacao.Fechado -> {}

        is AtalhoDeAnotacao.EscolhendoAluno -> {
            if (turmasDoInicio is TurmasDoInicio.Carregadas) {
                FolhaEscolhaDeAluno(
                    alunos = turmasDoInicio.alunos,
                    aoEscolher = { aluno -> atalhoDeAnotacao.value = AtalhoDeAnotacao.Escrevendo(aluno) },
                    aoFechar = { atalhoDeAnotacao.value = AtalhoDeAnotacao.Fechado },
                )
            }
        }

        is AtalhoDeAnotacao.Escrevendo -> FolhaAnotacao(
            titulo = stringResource(R.string.inicio_anotacao_sobre, atalho.aluno.nome),
            textoInicial = "",
            aoSalvar = { texto -> salvarAnotacao(atalho.aluno, texto) },
            aoExcluir = null,
            aoFechar = { atalhoDeAnotacao.value = AtalhoDeAnotacao.Fechado },
        )
    }
}

/** Saudação e data; à direita, a engrenagem das configurações (backup dos dados). */
@Composable
private fun Cabecalho(saudacao: Saudacao, hoje: LocalDate, aoAbrirConfiguracoes: () -> Unit) {
    Row(verticalAlignment = Alignment.Top) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = textoDaSaudacao(saudacao),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = formatarDataPorExtenso(hoje),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = aoAbrirConfiguracoes) {
            Icon(
                painter = painterResource(R.drawable.ic_configuracoes),
                contentDescription = stringResource(R.string.configuracoes_titulo),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun textoDaSaudacao(saudacao: Saudacao): String {
    val idDoTexto: Int = when (saudacao) {
        Saudacao.BOM_DIA -> R.string.saudacao_bom_dia
        Saudacao.BOA_TARDE -> R.string.saudacao_boa_tarde
        Saudacao.BOA_NOITE -> R.string.saudacao_boa_noite
    }
    return stringResource(idDoTexto)
}

@Composable
private fun AvisoSemTurmas(aoCadastrarTurma: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.inicio_sem_turmas),
            style = MaterialTheme.typography.bodyLarge,
        )
        Button(onClick = aoCadastrarTurma) {
            Text(stringResource(R.string.inicio_cadastrar_turma))
        }
    }
}
