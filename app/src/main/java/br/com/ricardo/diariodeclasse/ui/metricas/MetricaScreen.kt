package br.com.ricardo.diariodeclasse.ui.metricas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.ricardo.diariodeclasse.R
import br.com.ricardo.diariodeclasse.data.local.entity.SondagemResumida
import br.com.ricardo.diariodeclasse.ui.componentes.BarraSuperior
import br.com.ricardo.diariodeclasse.ui.componentes.DialogoCalendario
import br.com.ricardo.diariodeclasse.ui.componentes.TelaCarregando
import br.com.ricardo.diariodeclasse.ui.componentes.formatarDataComDiaDaSemana
import java.time.LocalDate

@Composable
fun MetricaScreen(
    aoEditarMetrica: (turmaId: String, metricaId: String) -> Unit,
    aoAbrirSondagem: (metricaId: String, data: LocalDate) -> Unit,
    aoVoltar: () -> Unit,
    viewModel: MetricaViewModel = hiltViewModel(),
) {
    val estado: MetricaUiState = viewModel.uiState.collectAsStateWithLifecycle().value

    when (estado) {
        is MetricaUiState.Carregando -> TelaCarregando()

        is MetricaUiState.MetricaNaoEncontrada -> {
            LaunchedEffect(Unit) {
                aoVoltar()
            }
        }

        is MetricaUiState.Carregado -> ConteudoMetrica(
            estado = estado,
            aoEditar = { aoEditarMetrica(estado.metrica.turmaId, estado.metrica.id) },
            aoAbrirSondagem = { data -> aoAbrirSondagem(estado.metrica.id, data) },
            aoVoltar = aoVoltar,
        )
    }
}

@Composable
private fun ConteudoMetrica(
    estado: MetricaUiState.Carregado,
    aoEditar: () -> Unit,
    aoAbrirSondagem: (data: LocalDate) -> Unit,
    aoVoltar: () -> Unit,
) {
    val escolhendoOutroDia: MutableState<Boolean> = remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            BarraSuperior(
                titulo = estado.metrica.nome,
                aoVoltar = aoVoltar,
                acoes = {
                    IconButton(onClick = aoEditar) {
                        Icon(
                            painter = painterResource(R.drawable.ic_editar),
                            contentDescription = stringResource(R.string.metrica_editar),
                        )
                    }
                },
            )
        },
        bottomBar = {
            if (estado.temAlunos) {
                BotaoSondagemDeHoje(
                    jaExiste = estado.temSondagemHoje(),
                    aoTocar = { aoAbrirSondagem(estado.hoje) },
                )
            }
        },
    ) { espacamentoDasBarras ->
        LazyColumn(
            contentPadding = PaddingValues(bottom = 16.dp),
            modifier = Modifier
                .padding(espacamentoDasBarras)
                .fillMaxSize(),
        ) {
            item(key = "distribuicao") {
                SecaoDaDistribuicao(estado)
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
            }

            item(key = "cabecalho_sondagens") {
                CabecalhoDasSondagens(
                    podeRegistrar = estado.temAlunos,
                    aoEscolherOutroDia = { escolhendoOutroDia.value = true },
                )
            }

            if (!estado.temAlunos) {
                item(key = "sem_alunos") {
                    TextoDiscreto(stringResource(R.string.metrica_sem_alunos))
                }
            } else if (estado.sondagens.isEmpty()) {
                item(key = "sem_sondagens") {
                    TextoDiscreto(stringResource(R.string.metrica_sem_sondagens))
                }
            }

            items(estado.sondagens, key = { sondagem -> sondagem.id }) { sondagem ->
                LinhaDaSondagem(
                    sondagem = sondagem,
                    hoje = estado.hoje,
                    aoTocar = { aoAbrirSondagem(sondagem.data) },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }

    if (escolhendoOutroDia.value) {
        DialogoCalendario(
            dataInicial = estado.hoje,
            ultimaDataPermitida = estado.hoje,
            aoEscolher = { data ->
                escolhendoOutroDia.value = false
                aoAbrirSondagem(data)
            },
            aoCancelar = { escolhendoOutroDia.value = false },
        )
    }
}

/**
 * Uma barra por nível com a quantidade de alunos, na ordem da escala: dá para
 * ver de relance onde a turma está concentrada.
 */
@Composable
private fun SecaoDaDistribuicao(estado: MetricaUiState.Carregado) {
    val total: Int = estado.totalDeAlunos()

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = stringResource(R.string.metrica_turma_hoje),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.secondary,
        )
        for (faixa in estado.distribuicao.faixas) {
            BarraDoNivel(nome = faixa.nivel.nome, quantidade = faixa.quantidade, total = total)
        }
        if (estado.distribuicao.semAvaliacao > 0) {
            BarraDoNivel(
                nome = stringResource(R.string.metrica_sem_avaliacao),
                quantidade = estado.distribuicao.semAvaliacao,
                total = total,
            )
        }
    }
}

@Composable
private fun BarraDoNivel(nome: String, quantidade: Int, total: Int) {
    val fracao: Float
    if (total == 0) {
        fracao = 0f
    } else {
        fracao = quantidade.toFloat() / total.toFloat()
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row {
            Text(text = nome, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(text = quantidade.toString(), style = MaterialTheme.typography.labelLarge)
        }
        LinearProgressIndicator(progress = { fracao }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun CabecalhoDasSondagens(podeRegistrar: Boolean, aoEscolherOutroDia: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(start = 16.dp, end = 4.dp, top = 8.dp),
    ) {
        Text(
            text = stringResource(R.string.metrica_sondagens),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(1f),
        )
        if (podeRegistrar) {
            TextButton(onClick = aoEscolherOutroDia) {
                Text(stringResource(R.string.metrica_outro_dia))
            }
        }
    }
}

/** "Ter., 15/09" e quantos alunos foram avaliados; tocar abre para conferir ou corrigir. */
@Composable
private fun LinhaDaSondagem(sondagem: SondagemResumida, hoje: LocalDate, aoTocar: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = aoTocar)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = formatarDataComDiaDaSemana(sondagem.data, hoje),
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = pluralStringResource(
                R.plurals.metrica_alunos_avaliados,
                sondagem.quantidadeDeAlunos,
                sondagem.quantidadeDeAlunos,
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TextoDiscreto(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

/** O caminho mais comum fica sempre à mão: a sondagem de hoje (nova ou para corrigir). */
@Composable
private fun BotaoSondagemDeHoje(jaExiste: Boolean, aoTocar: () -> Unit) {
    val texto: String
    if (jaExiste) {
        texto = stringResource(R.string.metrica_editar_sondagem_hoje)
    } else {
        texto = stringResource(R.string.metrica_registrar_sondagem_hoje)
    }

    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 4.dp) {
        Button(
            onClick = aoTocar,
            modifier = Modifier
                .navigationBarsPadding()
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(texto)
        }
    }
}
