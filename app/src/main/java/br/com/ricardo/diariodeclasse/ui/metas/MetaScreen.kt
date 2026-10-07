package br.com.ricardo.diariodeclasse.ui.metas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.ricardo.diariodeclasse.R
import br.com.ricardo.diariodeclasse.data.local.entity.NivelDaMetrica
import br.com.ricardo.diariodeclasse.ui.componentes.BarraSuperior
import br.com.ricardo.diariodeclasse.ui.componentes.TelaCarregando
import java.time.LocalDate

@Composable
fun MetaScreen(
    aoEditarMeta: (turmaId: String, metaId: String) -> Unit,
    aoAbrirSondagem: (metricaId: String, data: LocalDate) -> Unit,
    aoAbrirAluno: (alunoId: String) -> Unit,
    aoVoltar: () -> Unit,
    viewModel: MetaViewModel = hiltViewModel(),
) {
    val estado: MetaUiState = viewModel.uiState.collectAsStateWithLifecycle().value

    when (estado) {
        is MetaUiState.Carregando -> TelaCarregando()

        is MetaUiState.MetaNaoEncontrada -> {
            LaunchedEffect(Unit) {
                aoVoltar()
            }
        }

        is MetaUiState.Carregado -> ConteudoMeta(
            estado = estado,
            aoEditar = { aoEditarMeta(estado.meta.turmaId, estado.meta.id) },
            aoRegistrarSondagem = { aoAbrirSondagem(estado.metrica.id, estado.hoje) },
            aoAbrirAluno = aoAbrirAluno,
            aoEncerrar = { viewModel.encerrar() },
            aoReabrir = { viewModel.reabrir() },
            aoVoltar = aoVoltar,
        )
    }
}

@Composable
private fun ConteudoMeta(
    estado: MetaUiState.Carregado,
    aoEditar: () -> Unit,
    aoRegistrarSondagem: () -> Unit,
    aoAbrirAluno: (alunoId: String) -> Unit,
    aoEncerrar: () -> Unit,
    aoReabrir: () -> Unit,
    aoVoltar: () -> Unit,
) {
    Scaffold(
        topBar = {
            BarraSuperior(
                titulo = stringResource(R.string.meta_titulo),
                aoVoltar = aoVoltar,
                acoes = {
                    IconButton(onClick = aoEditar) {
                        Icon(
                            painter = painterResource(R.drawable.ic_editar),
                            contentDescription = stringResource(R.string.meta_editar),
                        )
                    }
                },
            )
        },
    ) { espacamentoDasBarras ->
        val modifier = Modifier.padding(espacamentoDasBarras)

        LazyColumn(
            contentPadding = PaddingValues(bottom = 16.dp),
            modifier = modifier.fillMaxSize(),
        ) {
            item(key = "cabecalho") {
                CabecalhoDaMeta(
                    estado = estado,
                    aoRegistrarSondagem = aoRegistrarSondagem,
                    aoEncerrar = aoEncerrar,
                    aoReabrir = aoReabrir,
                )
                HorizontalDivider()
            }

            if (estado.progresso.total() == 0) {
                item(key = "sem_alunos") {
                    Text(
                        text = stringResource(R.string.meta_sem_alunos),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }

            // Um grupo por situação, na ordem do enum: quem já chegou primeiro.
            for (situacao in SituacaoNaMeta.entries) {
                val alunos: List<AlunoNoProgresso> = estado.progresso.alunosNaSituacao(situacao)
                if (alunos.isEmpty()) {
                    continue
                }
                item(key = "grupo_${situacao.name}") {
                    CabecalhoDoGrupo(situacao = situacao, quantidade = alunos.size)
                }
                items(alunos, key = { item -> item.aluno.id }) { item ->
                    LinhaDoAluno(item = item, aoTocar = { aoAbrirAluno(item.aluno.id) })
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

/**
 * Descrição, métrica, "Chegaram a Alfabético: 4 de 11" com a barra, o prazo e as
 * ações. "Registrar sondagem" fica aqui porque é o que faz a meta andar.
 */
@Composable
private fun CabecalhoDaMeta(
    estado: MetaUiState.Carregado,
    aoRegistrarSondagem: () -> Unit,
    aoEncerrar: () -> Unit,
    aoReabrir: () -> Unit,
) {
    val encerrada: Boolean = estado.meta.encerradaEm != null
    val corDoPrazo: Color
    if (prazoVenceu(estado.meta, estado.hoje)) {
        corDoPrazo = MaterialTheme.colorScheme.error
    } else {
        corDoPrazo = MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(16.dp),
    ) {
        Text(text = estado.meta.descricao, style = MaterialTheme.typography.titleLarge)
        Text(
            text = estado.metrica.nome,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = textoDoProgresso(estado.progresso), style = MaterialTheme.typography.titleMedium)
        LinearProgressIndicator(
            progress = { fracaoAtingida(estado.progresso) },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = textoDoPrazo(estado.meta, estado.hoje),
            style = MaterialTheme.typography.labelLarge,
            color = corDoPrazo,
        )

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
            if (!encerrada) {
                OutlinedButton(onClick = aoRegistrarSondagem) {
                    Text(stringResource(R.string.meta_registrar_sondagem))
                }
            }
            Spacer(Modifier.weight(1f))
            if (encerrada) {
                TextButton(onClick = aoReabrir) {
                    Text(stringResource(R.string.meta_reabrir))
                }
            } else {
                TextButton(onClick = aoEncerrar) {
                    Text(stringResource(R.string.meta_encerrar))
                }
            }
        }
    }
}

@Composable
private fun CabecalhoDoGrupo(situacao: SituacaoNaMeta, quantidade: Int) {
    val titulo: String = when (situacao) {
        SituacaoNaMeta.ATINGIU -> stringResource(R.string.meta_situacao_atingiu)
        SituacaoNaMeta.AVANCOU -> stringResource(R.string.meta_situacao_avancou)
        SituacaoNaMeta.NAO_AVANCOU -> stringResource(R.string.meta_situacao_nao_avancou)
        SituacaoNaMeta.SEM_AVALIACAO -> stringResource(R.string.meta_situacao_sem_avaliacao)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp),
    ) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = quantidade.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

/** Nome (abre a página do aluno) e o nível atual dele. */
@Composable
private fun LinhaDoAluno(item: AlunoNoProgresso, aoTocar: () -> Unit) {
    val nivel: NivelDaMetrica? = item.nivelAtual

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = aoTocar)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = item.aluno.nome,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        if (nivel != null) {
            Text(
                text = nivel.nome,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
