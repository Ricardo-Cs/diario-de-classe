package br.com.ricardo.diariodeclasse.ui.inicio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.ricardo.diariodeclasse.R
import br.com.ricardo.diariodeclasse.ui.componentes.SeletorDeTurma
import br.com.ricardo.diariodeclasse.ui.componentes.formatarDataPorExtenso
import java.time.LocalDate

@Composable
fun InicioScreen(
    aoCadastrarTurma: () -> Unit,
    aoAbrirChamada: (turmaId: String, data: LocalDate) -> Unit,
    aoAbrirTurma: (turmaId: String) -> Unit,
    aoAbrirPendencias: (turmaId: String) -> Unit,
    viewModel: InicioViewModel = hiltViewModel(),
) {
    val estado: InicioUiState = viewModel.uiState.collectAsStateWithLifecycle().value

    // Roda toda vez que a tela volta ao primeiro plano (como o `onResume` de uma Activity).
    LifecycleResumeEffect(Unit) {
        viewModel.atualizarDataEHora()
        onPauseOrDispose { }
    }

    Scaffold { espacamentoDasBarras ->
        Column(
            modifier = Modifier
                .padding(espacamentoDasBarras)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Cabecalho(saudacao = estado.saudacao, hoje = estado.hoje)

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
                        aoAdicionarAlunos = { aoAbrirTurma(turmas.turmaAtiva.id) },
                    )
                    CardPendencias(
                        resumo = turmas.pendencias,
                        aoAbrirPendencias = { aoAbrirPendencias(turmas.turmaAtiva.id) },
                    )
                    SecaoAtividadeRecente(atividades = turmas.atividadeRecente, hoje = estado.hoje)
                }
            }
        }
    }
}

@Composable
private fun Cabecalho(saudacao: Saudacao, hoje: LocalDate) {
    Column {
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
