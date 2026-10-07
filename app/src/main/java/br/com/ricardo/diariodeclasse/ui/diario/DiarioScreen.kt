package br.com.ricardo.diariodeclasse.ui.diario

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.ricardo.diariodeclasse.R
import br.com.ricardo.diariodeclasse.ui.componentes.BarraSuperior
import br.com.ricardo.diariodeclasse.ui.componentes.MensagemCentralizada
import br.com.ricardo.diariodeclasse.ui.componentes.SeletorDeTurma
import br.com.ricardo.diariodeclasse.ui.componentes.TelaCarregando
import br.com.ricardo.diariodeclasse.ui.componentes.formatarDataCurta
import br.com.ricardo.diariodeclasse.ui.metas.fracaoAtingida
import br.com.ricardo.diariodeclasse.ui.metas.prazoVenceu
import br.com.ricardo.diariodeclasse.ui.metas.textoDoPrazo
import br.com.ricardo.diariodeclasse.ui.metas.textoDoProgresso
import br.com.ricardo.diariodeclasse.ui.metricas.DistribuicaoDaMetrica
import br.com.ricardo.diariodeclasse.ui.metricas.FaixaDaDistribuicao
import java.time.LocalDate

@Composable
fun DiarioScreen(
    aoCriarMetrica: (turmaId: String) -> Unit,
    aoAbrirMetrica: (metricaId: String) -> Unit,
    aoCriarMeta: (turmaId: String) -> Unit,
    aoAbrirMeta: (metaId: String) -> Unit,
    viewModel: DiarioViewModel = hiltViewModel(),
) {
    val estado: DiarioUiState = viewModel.uiState.collectAsStateWithLifecycle().value

    Scaffold(
        topBar = { BarraSuperior(titulo = stringResource(R.string.aba_diario)) },
    ) { espacamentoDasBarras ->
        val modifier = Modifier.padding(espacamentoDasBarras)

        when (estado) {
            is DiarioUiState.Carregando -> TelaCarregando()

            is DiarioUiState.NenhumaTurma -> {
                MensagemCentralizada(stringResource(R.string.diario_sem_turma), modifier)
            }

            is DiarioUiState.Carregado -> ConteudoDiario(
                estado = estado,
                aoSelecionarTurma = { turmaId -> viewModel.selecionarTurma(turmaId) },
                aoCriarMetrica = { aoCriarMetrica(estado.turmaAtiva.id) },
                aoAbrirMetrica = aoAbrirMetrica,
                aoCriarMeta = { aoCriarMeta(estado.turmaAtiva.id) },
                aoAbrirMeta = aoAbrirMeta,
                modifier = modifier,
            )
        }
    }
}

/** Metas primeiro: são o que tem prazo e pede ação. Depois as métricas que as alimentam. */
@Composable
private fun ConteudoDiario(
    estado: DiarioUiState.Carregado,
    aoSelecionarTurma: (turmaId: String) -> Unit,
    aoCriarMetrica: () -> Unit,
    aoAbrirMetrica: (metricaId: String) -> Unit,
    aoCriarMeta: () -> Unit,
    aoAbrirMeta: (metaId: String) -> Unit,
    modifier: Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        SeletorDeTurma(
            turmaAtiva = estado.turmaAtiva,
            todasAsTurmas = estado.todasAsTurmas,
            aoSelecionarTurma = aoSelecionarTurma,
        )

        SecaoDeMetas(
            estado = estado,
            aoCriarMeta = aoCriarMeta,
            aoAbrirMeta = aoAbrirMeta,
        )

        SecaoDeMetricas(
            estado = estado,
            aoCriarMetrica = aoCriarMetrica,
            aoAbrirMetrica = aoAbrirMetrica,
        )
    }
}

@Composable
private fun SecaoDeMetas(
    estado: DiarioUiState.Carregado,
    aoCriarMeta: () -> Unit,
    aoAbrirMeta: (metaId: String) -> Unit,
) {
    val podeCriarMeta: Boolean = estado.metricas.isNotEmpty()

    CabecalhoDaSecao(
        titulo = stringResource(R.string.diario_metas),
        descricaoDoBotao = stringResource(R.string.diario_nova_meta),
        botaoHabilitado = podeCriarMeta,
        aoAdicionar = aoCriarMeta,
    )

    if (!podeCriarMeta) {
        TextoDeSecaoVazia(stringResource(R.string.diario_metas_precisam_de_metrica))
    } else if (estado.metasEmAndamento.isEmpty()) {
        TextoDeSecaoVazia(stringResource(R.string.diario_sem_metas))
    }

    for (resumo in estado.metasEmAndamento) {
        CardDaMeta(resumo = resumo, hoje = estado.hoje, aoTocar = { aoAbrirMeta(resumo.meta.id) })
    }

    if (estado.metasEncerradas.isNotEmpty()) {
        Text(
            text = stringResource(R.string.diario_metas_encerradas),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        for (resumo in estado.metasEncerradas) {
            CardDaMeta(resumo = resumo, hoje = estado.hoje, aoTocar = { aoAbrirMeta(resumo.meta.id) })
        }
    }
}

/** Descrição, "Chegaram a Alfabético: 4 de 11", a barra de progresso e o prazo. */
@Composable
private fun CardDaMeta(resumo: ResumoDaMeta, hoje: LocalDate, aoTocar: () -> Unit) {
    val corDoPrazo: Color
    if (prazoVenceu(resumo.meta, hoje)) {
        corDoPrazo = MaterialTheme.colorScheme.error
    } else {
        corDoPrazo = MaterialTheme.colorScheme.onSurfaceVariant
    }

    OutlinedCard(
        onClick = aoTocar,
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            Text(text = resumo.meta.descricao, style = MaterialTheme.typography.titleMedium)
            Text(text = textoDoProgresso(resumo.progresso), style = MaterialTheme.typography.bodyMedium)
            LinearProgressIndicator(
                progress = { fracaoAtingida(resumo.progresso) },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = textoDoPrazo(resumo.meta, hoje),
                style = MaterialTheme.typography.labelMedium,
                color = corDoPrazo,
            )
        }
    }
}

@Composable
private fun SecaoDeMetricas(
    estado: DiarioUiState.Carregado,
    aoCriarMetrica: () -> Unit,
    aoAbrirMetrica: (metricaId: String) -> Unit,
) {
    CabecalhoDaSecao(
        titulo = stringResource(R.string.diario_metricas),
        descricaoDoBotao = stringResource(R.string.diario_nova_metrica),
        botaoHabilitado = true,
        aoAdicionar = aoCriarMetrica,
    )

    if (estado.metricas.isEmpty()) {
        TextoDeSecaoVazia(stringResource(R.string.diario_sem_metricas))
        Button(onClick = aoCriarMetrica) {
            Text(stringResource(R.string.diario_criar_metrica))
        }
        return
    }

    for (resumo in estado.metricas) {
        CardDaMetrica(resumo = resumo, hoje = estado.hoje, aoTocar = { aoAbrirMetrica(resumo.metrica.id) })
    }
}

/** Nome, retrato da turma ("14 Alfabético · 11 Silábico com valor sonoro") e a última sondagem. */
@Composable
private fun CardDaMetrica(resumo: ResumoDaMetrica, hoje: LocalDate, aoTocar: () -> Unit) {
    val ultimaSondagem: LocalDate? = resumo.ultimaSondagem
    val textoDaSondagem: String
    if (ultimaSondagem == null) {
        textoDaSondagem = stringResource(R.string.diario_nenhuma_sondagem)
    } else {
        textoDaSondagem = stringResource(R.string.diario_ultima_sondagem, formatarDataCurta(ultimaSondagem, hoje))
    }
    val retrato: String = textoDoRetrato(resumo.distribuicao)

    OutlinedCard(
        onClick = aoTocar,
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            Text(text = resumo.metrica.nome, style = MaterialTheme.typography.titleMedium)
            if (retrato.isNotEmpty()) {
                Text(text = retrato, style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                text = textoDaSondagem,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Só os níveis com alguém, do mais avançado para o mais inicial. Vazio antes da primeira sondagem. */
private fun textoDoRetrato(distribuicao: DistribuicaoDaMetrica): String {
    val partes = mutableListOf<String>()
    for (faixa: FaixaDaDistribuicao in distribuicao.faixas.reversed()) {
        if (faixa.quantidade > 0) {
            partes.add("${faixa.quantidade} ${faixa.nivel.nome}")
        }
    }
    return partes.joinToString(separator = " · ")
}

@Composable
private fun CabecalhoDaSecao(
    titulo: String,
    descricaoDoBotao: String,
    botaoHabilitado: Boolean,
    aoAdicionar: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    ) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = aoAdicionar, enabled = botaoHabilitado) {
            Icon(
                painter = painterResource(R.drawable.ic_adicionar),
                contentDescription = descricaoDoBotao,
            )
        }
    }
}

@Composable
private fun TextoDeSecaoVazia(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
