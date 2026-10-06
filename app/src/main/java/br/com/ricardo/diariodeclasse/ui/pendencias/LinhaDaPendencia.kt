package br.com.ricardo.diariodeclasse.ui.pendencias

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.com.ricardo.diariodeclasse.R
import br.com.ricardo.diariodeclasse.data.local.entity.PendenciaComOrigem
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Uma pendência: atividade, quando lembrar e de onde veio, com o botão de
 * entregue. Usada na tela de pendências da turma e na tela do aluno.
 * Tocar na linha abre a edição.
 */
@Composable
fun LinhaDaPendencia(
    item: PendenciaComOrigem,
    hoje: LocalDate,
    aoEditar: () -> Unit,
    aoMarcarComoEntregue: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable(onClick = aoEditar)
            .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = item.pendencia.descricao, style = MaterialTheme.typography.bodyLarge)
            Row {
                TextoDoLembrete(dataLembrete = item.pendencia.dataLembrete, hoje = hoje)
                Text(
                    text = " · " + textoDaOrigem(item.dataDaFalta),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
 * - hoje: "Para hoje" (azul-marinho: destaque sem alarme);
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
        cor = MaterialTheme.colorScheme.secondary
    } else if (dataLembrete == hoje.plusDays(1)) {
        texto = stringResource(R.string.pendencia_lembrete_amanha)
        cor = MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        texto = stringResource(R.string.pendencia_lembrete_data, dataCurta)
        cor = MaterialTheme.colorScheme.onSurfaceVariant
    }

    Text(text = texto, style = MaterialTheme.typography.bodySmall, color = cor)
}
