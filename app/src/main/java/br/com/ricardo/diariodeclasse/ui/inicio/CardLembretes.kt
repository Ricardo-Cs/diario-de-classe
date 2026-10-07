package br.com.ricardo.diariodeclasse.ui.inicio

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.com.ricardo.diariodeclasse.R
import br.com.ricardo.diariodeclasse.data.local.entity.Lembrete
import br.com.ricardo.diariodeclasse.ui.lembretes.LinhaDoLembrete
import java.time.LocalDate

/**
 * Lembretes da professora que pedem atenção agora (atrasados e próximos dias).
 * Sem nenhum, vira uma linha curta com o acesso à lista, como o card de pendências.
 * Tocar num lembrete abre a lista completa, onde ele pode ser editado.
 */
@Composable
fun CardLembretes(
    lembretes: List<Lembrete>,
    hoje: LocalDate,
    aoConcluir: (lembrete: Lembrete) -> Unit,
    aoAbrirLembretes: () -> Unit,
) {
    if (lembretes.isEmpty()) {
        LinhaSemLembretes(aoAbrirLembretes)
        return
    }

    OutlinedCard(
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)) {
            Text(
                text = stringResource(R.string.inicio_lembretes_titulo),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            for (lembrete in lembretes) {
                LinhaDoLembrete(
                    lembrete = lembrete,
                    hoje = hoje,
                    aoEditar = aoAbrirLembretes,
                    aoAlternarConclusao = { aoConcluir(lembrete) },
                    // Afasta o botão de concluir da borda do card.
                    modifier = Modifier.padding(end = 8.dp),
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            Row(modifier = Modifier.fillMaxWidth().padding(end = 8.dp)) {
                Spacer(Modifier.weight(1f))
                TextButton(onClick = aoAbrirLembretes) {
                    Text(stringResource(R.string.inicio_ver_todos_lembretes))
                }
            }
        }
    }
}

@Composable
private fun LinhaSemLembretes(aoAbrirLembretes: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.inicio_nenhum_lembrete),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = aoAbrirLembretes) {
            Text(stringResource(R.string.inicio_ver_lembretes))
        }
    }
}
