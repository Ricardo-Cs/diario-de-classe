package br.com.ricardo.diariodeclasse.ui.inicio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.ricardo.diariodeclasse.R
import br.com.ricardo.diariodeclasse.data.repository.AtividadeRecente
import br.com.ricardo.diariodeclasse.ui.componentes.textoDeDataRelativa
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Últimos registros da professora, para conferência rápida. Discreta de propósito:
 * sem card, textos pequenos, e some quando não há nada.
 */
@Composable
fun SecaoAtividadeRecente(atividades: List<AtividadeRecente>, hoje: LocalDate) {
    if (atividades.isEmpty()) {
        return
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(top = 8.dp),
    ) {
        Text(
            text = stringResource(R.string.inicio_atividade_recente),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        for (atividade in atividades) {
            LinhaDeAtividade(atividade = atividade, hoje = hoje)
        }
    }
}

@Composable
private fun LinhaDeAtividade(atividade: AtividadeRecente, hoje: LocalDate) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tituloDaAtividade(atividade),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = detalheDaAtividade(atividade),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = quandoAconteceu(atividade, hoje),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Ex.: "Chamada editada", "Pendência entregue", "Anotação registrada". */
@Composable
private fun tituloDaAtividade(atividade: AtividadeRecente): String {
    val idDoTexto: Int = when (atividade) {
        is AtividadeRecente.Chamada -> {
            if (atividade.editada) R.string.atividade_chamada_editada else R.string.atividade_chamada_registrada
        }
        is AtividadeRecente.PendenciaRegistrada -> {
            if (atividade.editada) R.string.atividade_pendencia_editada else R.string.atividade_pendencia_criada
        }
        is AtividadeRecente.PendenciaEntregue -> R.string.atividade_pendencia_entregue
        is AtividadeRecente.Anotacao -> {
            if (atividade.editada) R.string.atividade_anotacao_editada else R.string.atividade_anotacao_registrada
        }
    }
    return stringResource(idDoTexto)
}

/** Ex.: "2 ausentes", "Maria · Ficha de português". */
@Composable
private fun detalheDaAtividade(atividade: AtividadeRecente): String {
    return when (atividade) {
        is AtividadeRecente.Chamada -> {
            if (atividade.ausentes == 0) {
                stringResource(R.string.chamada_nenhum_ausente)
            } else {
                pluralStringResource(R.plurals.chamada_ausentes, atividade.ausentes, atividade.ausentes)
            }
        }
        is AtividadeRecente.PendenciaRegistrada -> atividade.nomeDoAluno + " · " + atividade.descricao
        is AtividadeRecente.PendenciaEntregue -> atividade.nomeDoAluno + " · " + atividade.descricao
        is AtividadeRecente.Anotacao -> atividade.nomeDoAluno + " · " + atividade.texto
    }
}

/** Hoje mostra só a hora ("10:33"); antes, o dia ("Ontem", "02/10"). */
@Composable
private fun quandoAconteceu(atividade: AtividadeRecente, hoje: LocalDate): String {
    val dia: LocalDate = atividade.momento.toLocalDate()
    if (dia == hoje) {
        return atividade.momento.format(DateTimeFormatter.ofPattern("HH:mm"))
    }
    return textoDeDataRelativa(dia, hoje)
}
