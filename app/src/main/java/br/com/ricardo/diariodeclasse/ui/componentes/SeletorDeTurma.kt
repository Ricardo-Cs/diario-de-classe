package br.com.ricardo.diariodeclasse.ui.componentes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.com.ricardo.diariodeclasse.R
import br.com.ricardo.diariodeclasse.data.local.entity.Turma

/**
 * Botão com a turma ativa; ao tocar, abre um menu com as demais turmas.
 * Com uma turma só não há o que trocar: mostra apenas o nome, sem seta nem menu.
 * Usado em todas as abas (Início, A fazer, Turma e Acompanhamento), que partem da mesma turma ativa.
 *
 * `remember { mutableStateOf(...) }` é o "useState" do Compose: guarda se o menu
 * está aberto e redesenha o componente quando o valor muda.
 */
@Composable
fun SeletorDeTurma(
    turmaAtiva: Turma,
    todasAsTurmas: List<Turma>,
    aoSelecionarTurma: (turmaId: String) -> Unit,
) {
    if (todasAsTurmas.size < 2) {
        NomeDaTurmaAtiva(turmaAtiva)
        return
    }

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

/** Mesmo ícone e texto do botão do seletor, sem parecer tocável. */
@Composable
private fun NomeDaTurmaAtiva(turma: Turma) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(R.drawable.ic_turma),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = descricaoDaTurma(turma),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** O período ajuda a diferenciar turmas de mesmo nome (ex.: 1º ano A manhã e tarde). */
@Composable
private fun descricaoDaTurma(turma: Turma): String {
    return "${turma.nome} · ${nomeDoPeriodo(turma.periodo)}"
}
