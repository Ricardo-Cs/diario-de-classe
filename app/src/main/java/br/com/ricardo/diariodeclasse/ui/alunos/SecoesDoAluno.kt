package br.com.ricardo.diariodeclasse.ui.alunos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.ricardo.diariodeclasse.R
import br.com.ricardo.diariodeclasse.data.local.entity.Aluno
import br.com.ricardo.diariodeclasse.data.local.entity.Anotacao
import br.com.ricardo.diariodeclasse.data.local.entity.FaltaDoAluno
import br.com.ricardo.diariodeclasse.data.local.entity.Pendencia
import br.com.ricardo.diariodeclasse.data.local.entity.PendenciaComOrigem
import br.com.ricardo.diariodeclasse.ui.componentes.formatarDataComDiaDaSemana
import br.com.ricardo.diariodeclasse.ui.componentes.formatarDataCurta
import br.com.ricardo.diariodeclasse.ui.componentes.nomeDoMes
import br.com.ricardo.diariodeclasse.ui.componentes.textoDeDataRelativa
import br.com.ricardo.diariodeclasse.ui.pendencias.LinhaDaPendencia
import br.com.ricardo.diariodeclasse.ui.theme.ausencia
import java.time.LocalDate

/*
 * As seções da tela do aluno. Cada uma é uma função de extensão de
 * `LazyListScope` (o "this" dentro de `LazyColumn { ... }`): assim cada seção
 * acrescenta os próprios itens à lista, e a tela só chama
 * `secaoDePendencias(...)`, `secaoDeFaltas(...)` etc. em sequência.
 */

/** O que está pendente vem primeiro: é o que pede ação da professora. */
fun LazyListScope.secaoDePendencias(
    aluno: Aluno,
    pendencias: List<PendenciaComOrigem>,
    hoje: LocalDate,
    aoAdicionar: () -> Unit,
    aoEditar: (pendencia: Pendencia) -> Unit,
    aoMarcarComoEntregue: (pendencia: Pendencia) -> Unit,
) {
    item(key = "cabecalho_pendencias") {
        CabecalhoDaSecao(titulo = stringResource(R.string.aluno_pendencias)) {
            IconButton(onClick = aoAdicionar) {
                Icon(
                    painter = painterResource(R.drawable.ic_adicionar),
                    contentDescription = stringResource(R.string.pendencia_adicionar_para, aluno.nome),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (pendencias.isEmpty()) {
        item(key = "sem_pendencias") {
            TextoDeSecaoVazia(stringResource(R.string.aluno_sem_pendencias))
        }
    } else {
        items(pendencias, key = { item -> item.pendencia.id }) { item ->
            LinhaDaPendencia(
                item = item,
                hoje = hoje,
                aoEditar = { aoEditar(item.pendencia) },
                aoMarcarComoEntregue = { aoMarcarComoEntregue(item.pendencia) },
            )
        }
    }

    item(key = "divisor_pendencias") {
        DivisorDeSecao()
    }
}

/** Faltas recentes, só para consulta: a edição continua sendo feita na chamada. */
fun LazyListScope.secaoDeFaltas(resumo: ResumoDasFaltas, hoje: LocalDate) {
    item(key = "cabecalho_faltas") {
        CabecalhoDaSecao(titulo = stringResource(R.string.aluno_faltas)) {
            TextoDasFaltasNoMes(noMes = resumo.noMes, hoje = hoje)
        }
    }

    if (resumo.recentes.isEmpty()) {
        item(key = "sem_faltas") {
            TextoDeSecaoVazia(stringResource(R.string.aluno_sem_faltas))
        }
    } else {
        items(resumo.recentes) { falta ->
            LinhaDaFalta(falta = falta, hoje = hoje)
        }
    }

    if (resumo.naoMostradas > 0) {
        item(key = "faltas_mais_antigas") {
            Text(
                text = pluralStringResource(R.plurals.aluno_faltas_mais_antigas, resumo.naoMostradas, resumo.naoMostradas),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
    }

    item(key = "divisor_faltas") {
        DivisorDeSecao()
    }
}

/**
 * Nível atual em cada métrica e os níveis anteriores. Aluno ainda não avaliado
 * em nenhuma métrica não mostra a seção: ela só ocuparia espaço.
 */
fun LazyListScope.secaoDeAcompanhamento(evolucoes: List<EvolucaoNaMetrica>, hoje: LocalDate) {
    if (evolucoes.isEmpty()) {
        return
    }

    item(key = "cabecalho_acompanhamento") {
        CabecalhoDaSecao(titulo = stringResource(R.string.aluno_acompanhamento))
    }

    items(evolucoes, key = { evolucao -> "evolucao_${evolucao.metricaId}" }) { evolucao ->
        ItemEvolucao(evolucao = evolucao, hoje = hoje)
    }

    item(key = "divisor_acompanhamento") {
        DivisorDeSecao()
    }
}

/** Só os passos mais recentes, para a seção não crescer a cada sondagem do ano. */
private const val MAXIMO_DE_PASSOS_ANTERIORES = 3

@Composable
private fun ItemEvolucao(evolucao: EvolucaoNaMetrica, hoje: LocalDate) {
    val atual: PassoDaEvolucao = evolucao.passos.first()
    val anteriores: List<PassoDaEvolucao> = evolucao.passos.drop(1).take(MAXIMO_DE_PASSOS_ANTERIORES)

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        Text(
            text = evolucao.nomeDaMetrica,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(text = atual.nomeDoNivel, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.aluno_nivel_desde, formatarDataCurta(atual.desde, hoje)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        for (passo in anteriores) {
            Text(
                text = stringResource(R.string.aluno_nivel_anterior, passo.nomeDoNivel, formatarDataCurta(passo.desde, hoje)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

fun LazyListScope.secaoDeAnotacoes(
    aluno: Aluno,
    anotacoes: List<Anotacao>,
    hoje: LocalDate,
    aoTocar: (anotacao: Anotacao) -> Unit,
) {
    item(key = "cabecalho_anotacoes") {
        CabecalhoDaSecao(titulo = stringResource(R.string.aluno_anotacoes))
    }

    if (anotacoes.isEmpty()) {
        item(key = "sem_anotacoes") {
            TextoDeSecaoVazia(stringResource(R.string.aluno_sem_anotacoes, aluno.nome))
        }
        return
    }

    items(anotacoes, key = { anotacao -> anotacao.id }) { anotacao ->
        ItemAnotacao(anotacao = anotacao, hoje = hoje, aoTocar = { aoTocar(anotacao) })
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

/**
 * Título da seção à esquerda e, à direita, um complemento opcional (botão ou texto).
 * A altura mínima de 48dp mantém os cabeçalhos alinhados, com ou sem botão.
 */
@Composable
private fun CabecalhoDaSecao(
    titulo: String,
    complemento: @Composable () -> Unit = {},
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(start = 16.dp, end = 4.dp, top = 8.dp),
    ) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(1f),
        )
        complemento()
    }
}

/** "3 em outubro" em ocre, ou "Nenhuma em outubro" discreto. */
@Composable
private fun TextoDasFaltasNoMes(noMes: Int, hoje: LocalDate) {
    val mes: String = nomeDoMes(hoje)
    val modifier = Modifier.padding(end = 12.dp)

    if (noMes == 0) {
        Text(
            text = stringResource(R.string.aluno_nenhuma_falta_no_mes, mes),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
        )
    } else {
        Text(
            text = stringResource(R.string.aluno_faltas_no_mes, noMes, mes),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.ausencia,
            modifier = modifier,
        )
    }
}

/** "Seg., 05/10" e, ao lado, a observação da falta, se houver. */
@Composable
private fun LinhaDaFalta(falta: FaltaDoAluno, hoje: LocalDate) {
    Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        Text(
            text = formatarDataComDiaDaSemana(falta.data, hoje),
            style = MaterialTheme.typography.bodyMedium,
        )
        val observacao: String? = falta.observacao
        if (observacao != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = "· $observacao",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Data em destaque discreto ("Hoje", "Ontem", "02/10") e o texto completo abaixo. */
@Composable
private fun ItemAnotacao(anotacao: Anotacao, hoje: LocalDate, aoTocar: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = aoTocar)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = textoDeDataRelativa(anotacao.data, hoje),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = anotacao.texto, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun TextoDeSecaoVazia(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun DivisorDeSecao() {
    HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
}
