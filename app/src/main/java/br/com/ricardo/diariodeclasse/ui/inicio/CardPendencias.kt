package br.com.ricardo.diariodeclasse.ui.inicio

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.ricardo.diariodeclasse.R
import br.com.ricardo.diariodeclasse.ui.pendencias.textoDaOrigem

/**
 * Pendências do dia no Início. Sem nada para hoje, vira uma linha curta e positiva
 * em vez de um card vazio ocupando espaço.
 */
@Composable
fun CardPendencias(
    resumo: ResumoDePendencias,
    aoAbrirPendencias: () -> Unit,
) {
    if (resumo.paraHoje == 0) {
        LinhaSemPendenciasParaHoje(aoAbrirPendencias)
        return
    }

    OutlinedCard(
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 4.dp)) {
            Text(
                text = stringResource(R.string.inicio_pendencias_titulo),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = pluralStringResource(R.plurals.inicio_pendencias_para_hoje, resumo.paraHoje, resumo.paraHoje),
                style = MaterialTheme.typography.titleLarge,
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 12.dp),
            ) {
                for (lembrete in resumo.lembretes) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    LinhaDoLembrete(lembrete = lembrete, aoTocar = aoAbrirPendencias)
                }
            }

            RodapeDoCard(resumo = resumo, aoVerTodas = aoAbrirPendencias)
        }
    }
}

@Composable
private fun LinhaDoLembrete(lembrete: LembreteDePendencia, aoTocar: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = aoTocar),
    ) {
        Text(
            text = lembrete.nomeDoAluno,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.secondary,
        )
        Text(
            text = lembrete.descricao + " · " + textoDaOrigem(lembrete.dataDaFalta),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** "e mais 2" quando há pendências de hoje além das mostradas, e o "Ver todas". */
@Composable
private fun RodapeDoCard(resumo: ResumoDePendencias, aoVerTodas: () -> Unit) {
    val naoMostradas: Int = resumo.paraHoje - resumo.lembretes.size

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        if (naoMostradas > 0) {
            Text(
                text = stringResource(R.string.inicio_pendencias_e_mais, naoMostradas),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.weight(1f))
        TextButton(onClick = aoVerTodas) {
            Text(stringResource(R.string.inicio_ver_todas))
        }
    }
}

@Composable
private fun LinhaSemPendenciasParaHoje(aoAbrirPendencias: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(
            painter = painterResource(R.drawable.ic_presente),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.inicio_nenhuma_pendencia_hoje),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = aoAbrirPendencias) {
            Text(stringResource(R.string.inicio_ver_pendencias))
        }
    }
}
