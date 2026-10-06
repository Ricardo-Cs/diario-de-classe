package br.com.ricardo.diariodeclasse.ui.inicio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.com.ricardo.diariodeclasse.R
import br.com.ricardo.diariodeclasse.ui.componentes.DialogoCalendario
import br.com.ricardo.diariodeclasse.ui.theme.ausencia
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Principal elemento do Início. Muda conforme a chamada do dia:
 * ainda não feita (com botão de destaque), já feita (resumo + editar)
 * ou impossível de fazer porque a turma não tem alunos.
 *
 * "Outro dia" abre o calendário para registrar uma chamada esquecida ou corrigir
 * uma antiga. Só aceita datas até hoje: não existe chamada de amanhã.
 */
@Composable
fun CardChamada(
    situacao: SituacaoDaChamada,
    hoje: LocalDate,
    aoAbrirChamada: () -> Unit,
    aoAbrirChamadaDeOutroDia: (data: LocalDate) -> Unit,
    aoAdicionarAlunos: () -> Unit,
) {
    val calendarioAberto: MutableState<Boolean> = remember { mutableStateOf(false) }
    val turmaTemAlunos: Boolean = situacao !is SituacaoDaChamada.TurmaSemAlunos

    OutlinedCard(
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.inicio_chamada_de_hoje),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                if (turmaTemAlunos) {
                    TextButton(onClick = { calendarioAberto.value = true }) {
                        Text(stringResource(R.string.inicio_chamada_outro_dia))
                    }
                }
            }

            when (situacao) {
                is SituacaoDaChamada.TurmaSemAlunos -> ConteudoTurmaSemAlunos(aoAdicionarAlunos)
                is SituacaoDaChamada.NaoFeita -> ConteudoChamadaNaoFeita(situacao, aoAbrirChamada)
                is SituacaoDaChamada.Feita -> ConteudoChamadaFeita(situacao, aoAbrirChamada)
            }
        }
    }

    if (calendarioAberto.value) {
        // Começa em ontem: o caso mais comum é a chamada esquecida do dia anterior.
        DialogoCalendario(
            dataInicial = hoje.minusDays(1),
            ultimaDataPermitida = hoje,
            aoEscolher = { data ->
                calendarioAberto.value = false
                aoAbrirChamadaDeOutroDia(data)
            },
            aoCancelar = { calendarioAberto.value = false },
        )
    }
}

@Composable
private fun ConteudoChamadaNaoFeita(situacao: SituacaoDaChamada.NaoFeita, aoFazerChamada: () -> Unit) {
    val total: Int = situacao.totalDeAlunos

    Text(
        text = stringResource(R.string.inicio_chamada_nao_feita),
        style = MaterialTheme.typography.titleLarge,
    )
    Text(
        text = pluralStringResource(R.plurals.inicio_alunos_na_turma, total, total),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Button(
        onClick = aoFazerChamada,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
    ) {
        Text(stringResource(R.string.inicio_fazer_chamada))
    }
}

@Composable
private fun ConteudoChamadaFeita(situacao: SituacaoDaChamada.Feita, aoEditar: () -> Unit) {
    val formatoDoHorario: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val horario: String = situacao.horario.format(formatoDoHorario)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(R.drawable.ic_presente),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = stringResource(R.string.inicio_chamada_concluida, horario),
            style = MaterialTheme.typography.titleLarge,
        )
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        ResumoDaChamada(presentes = situacao.presentes, ausentes = situacao.ausentes)
        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = aoEditar) {
            Text(stringResource(R.string.editar))
        }
    }
}

/** "22 presentes · 3 ausentes", com os ausentes em destaque (ocre) quando houver. */
@Composable
private fun ResumoDaChamada(presentes: Int, ausentes: Int) {
    val textoDosAusentes: String
    val corDosAusentes: Color
    if (ausentes > 0) {
        textoDosAusentes = pluralStringResource(R.plurals.chamada_ausentes, ausentes, ausentes)
        corDosAusentes = MaterialTheme.colorScheme.ausencia
    } else {
        textoDosAusentes = stringResource(R.string.chamada_nenhum_ausente)
        corDosAusentes = MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row {
        Text(
            text = pluralStringResource(R.plurals.inicio_presentes, presentes, presentes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = " · ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = textoDosAusentes,
            style = MaterialTheme.typography.bodyMedium,
            color = corDosAusentes,
        )
    }
}

@Composable
private fun ConteudoTurmaSemAlunos(aoAdicionarAlunos: () -> Unit) {
    Text(
        text = stringResource(R.string.turma_alunos_vazio),
        style = MaterialTheme.typography.bodyLarge,
    )
    OutlinedButton(
        onClick = aoAdicionarAlunos,
        modifier = Modifier.padding(top = 8.dp),
    ) {
        Text(stringResource(R.string.turma_adicionar_alunos))
    }
}
