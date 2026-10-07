package br.com.ricardo.diariodeclasse.ui.lembretes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import br.com.ricardo.diariodeclasse.R
import br.com.ricardo.diariodeclasse.data.local.entity.Lembrete
import br.com.ricardo.diariodeclasse.ui.componentes.formatarDataCurta
import java.time.LocalDate

/**
 * Um lembrete: o que fazer e quando, com o botão de concluir (mesmo padrão da
 * pendência). Concluído, o texto fica riscado e o botão passa a reabrir.
 * Tocar na linha abre a edição.
 */
@Composable
fun LinhaDoLembrete(
    lembrete: Lembrete,
    hoje: LocalDate,
    aoEditar: () -> Unit,
    aoAlternarConclusao: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val concluido: Boolean = lembrete.concluidoEm != null

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = aoEditar)
            .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (concluido) {
                Text(
                    text = lembrete.descricao,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textDecoration = TextDecoration.LineThrough,
                )
                Text(
                    text = formatarDataCurta(lembrete.data, hoje),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(text = lembrete.descricao, style = MaterialTheme.typography.bodyLarge)
                TextoDoQuando(data = lembrete.data, hoje = hoje)
            }
        }

        if (concluido) {
            IconButton(onClick = aoAlternarConclusao) {
                Icon(
                    painter = painterResource(R.drawable.ic_presente),
                    contentDescription = stringResource(R.string.lembrete_reabrir),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            FilledTonalIconButton(onClick = aoAlternarConclusao) {
                Icon(
                    painter = painterResource(R.drawable.ic_presente),
                    contentDescription = stringResource(R.string.lembrete_marcar_feito),
                )
            }
        }
    }
}

/**
 * - atrasado: "Atrasado há 2 dias (05/10)", em vermelho;
 * - hoje: "Hoje", em azul-marinho (destaque sem alarme);
 * - depois: "Amanhã" ou "Em 5 dias (12/10)", discreto.
 */
@Composable
private fun TextoDoQuando(data: LocalDate, hoje: LocalDate) {
    val dataCurta: String = formatarDataCurta(data, hoje)
    val texto: String
    val cor: Color
    when (val quando: QuandoDoLembrete = quandoDoLembrete(data, hoje)) {
        is QuandoDoLembrete.Atrasado -> {
            texto = pluralStringResource(R.plurals.lembrete_atrasado, quando.dias, quando.dias, dataCurta)
            cor = MaterialTheme.colorScheme.error
        }
        is QuandoDoLembrete.Hoje -> {
            texto = stringResource(R.string.lembrete_hoje)
            cor = MaterialTheme.colorScheme.secondary
        }
        is QuandoDoLembrete.Amanha -> {
            texto = stringResource(R.string.lembrete_amanha)
            cor = MaterialTheme.colorScheme.onSurfaceVariant
        }
        is QuandoDoLembrete.EmDias -> {
            texto = pluralStringResource(R.plurals.lembrete_em_dias, quando.dias, quando.dias, dataCurta)
            cor = MaterialTheme.colorScheme.onSurfaceVariant
        }
    }

    Text(text = texto, style = MaterialTheme.typography.bodySmall, color = cor)
}
