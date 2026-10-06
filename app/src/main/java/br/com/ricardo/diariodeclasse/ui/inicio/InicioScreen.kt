package br.com.ricardo.diariodeclasse.ui.inicio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.ricardo.diariodeclasse.R
import br.com.ricardo.diariodeclasse.data.local.entity.Turma
import br.com.ricardo.diariodeclasse.ui.componentes.formatarDataPorExtenso
import br.com.ricardo.diariodeclasse.ui.componentes.nomeDoPeriodo
import java.time.LocalDate

@Composable
fun InicioScreen(
    aoCadastrarTurma: () -> Unit,
    aoAbrirChamada: (turmaId: String, data: LocalDate) -> Unit,
    aoAbrirTurma: (turmaId: String) -> Unit,
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
                        aoAbrirChamada = { aoAbrirChamada(turmas.turmaAtiva.id, estado.hoje) },
                        aoAdicionarAlunos = { aoAbrirTurma(turmas.turmaAtiva.id) },
                    )
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

/**
 * Botão com a turma ativa; ao tocar, abre um menu com as demais turmas.
 * `remember { mutableStateOf(...) }` é o "useState" do Compose: guarda se o menu
 * está aberto e redesenha o componente quando o valor muda.
 */
@Composable
private fun SeletorDeTurma(
    turmaAtiva: Turma,
    todasAsTurmas: List<Turma>,
    aoSelecionarTurma: (turmaId: String) -> Unit,
) {
    val menuAberto: MutableState<Boolean> = remember { mutableStateOf(false) }

    Box {
        OutlinedButton(onClick = { menuAberto.value = true }) {
            Icon(painterResource(R.drawable.ic_turma), contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(descricaoDaTurma(turmaAtiva))
            Icon(
                painter = painterResource(R.drawable.ic_expandir),
                contentDescription = stringResource(R.string.inicio_trocar_turma),
            )
        }

        DropdownMenu(
            expanded = menuAberto.value,
            onDismissRequest = { menuAberto.value = false },
        ) {
            for (turma in todasAsTurmas) {
                DropdownMenuItem(
                    text = { Text(descricaoDaTurma(turma)) },
                    onClick = {
                        aoSelecionarTurma(turma.id)
                        menuAberto.value = false
                    },
                )
            }
        }
    }
}

/** O período ajuda a diferenciar turmas de mesmo nome (ex.: 1º ano A manhã e tarde). */
@Composable
private fun descricaoDaTurma(turma: Turma): String {
    return "${turma.nome} · ${nomeDoPeriodo(turma.periodo)}"
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
