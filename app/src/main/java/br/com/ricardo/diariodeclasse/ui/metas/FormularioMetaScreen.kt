package br.com.ricardo.diariodeclasse.ui.metas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.ricardo.diariodeclasse.R
import br.com.ricardo.diariodeclasse.data.local.entity.Metrica
import br.com.ricardo.diariodeclasse.data.local.entity.NivelDaMetrica
import br.com.ricardo.diariodeclasse.ui.componentes.BarraSuperior
import br.com.ricardo.diariodeclasse.ui.componentes.DialogoCalendario
import br.com.ricardo.diariodeclasse.ui.componentes.DialogoConfirmacao
import br.com.ricardo.diariodeclasse.ui.componentes.OpcaoDoMenu
import br.com.ricardo.diariodeclasse.ui.componentes.SeletorEmMenu
import br.com.ricardo.diariodeclasse.ui.componentes.TelaCarregando
import br.com.ricardo.diariodeclasse.ui.componentes.formatarDataPorExtenso
import java.time.LocalDate

@Composable
fun FormularioMetaScreen(
    aoVoltar: () -> Unit,
    aoExcluirMeta: () -> Unit,
    viewModel: FormularioMetaViewModel = hiltViewModel(),
) {
    val estado: FormularioMetaUiState = viewModel.uiState.collectAsStateWithLifecycle().value

    LaunchedEffect(estado.salvo) {
        if (estado.salvo) {
            aoVoltar()
        }
    }
    LaunchedEffect(estado.excluida) {
        if (estado.excluida) {
            aoExcluirMeta()
        }
    }

    if (estado.carregando) {
        TelaCarregando()
        return
    }
    ConteudoFormularioMeta(estado = estado, viewModel = viewModel, aoVoltar = aoVoltar)
}

@Composable
private fun ConteudoFormularioMeta(
    estado: FormularioMetaUiState,
    viewModel: FormularioMetaViewModel,
    aoVoltar: () -> Unit,
) {
    val escolhendoPrazo: MutableState<Boolean> = remember { mutableStateOf(false) }
    val confirmandoExclusao: MutableState<Boolean> = remember { mutableStateOf(false) }

    val titulo: String
    if (estado.editando) {
        titulo = stringResource(R.string.meta_formulario_titulo_edicao)
    } else {
        titulo = stringResource(R.string.meta_formulario_titulo_nova)
    }

    Scaffold(
        topBar = { BarraSuperior(titulo = titulo, aoVoltar = aoVoltar) },
        bottomBar = {
            BotaoSalvar(habilitado = estado.podeSalvar(), aoSalvar = { viewModel.salvar() })
        },
    ) { espacamentoDasBarras ->
        // Lista preguiçosa porque a turma inteira entra no fim do formulário.
        LazyColumn(
            contentPadding = PaddingValues(bottom = 16.dp),
            modifier = Modifier
                .padding(espacamentoDasBarras)
                .fillMaxSize(),
        ) {
            item(key = "campos") {
                CamposDaMeta(
                    estado = estado,
                    viewModel = viewModel,
                    aoEscolherPrazo = { escolhendoPrazo.value = true },
                )
            }

            item(key = "cabecalho_alunos") {
                CabecalhoDosAlunos(estado = estado, viewModel = viewModel)
                HorizontalDivider()
            }

            items(estado.alunos, key = { item -> item.aluno.id }) { item ->
                LinhaDoAluno(
                    item = item,
                    mostrarNivel = estado.forma == FormaDeAcompanhar.POR_METRICA,
                    aoAlternar = { viewModel.alternarAluno(item.aluno.id) },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }

            if (estado.editando) {
                item(key = "excluir") {
                    TextButton(
                        onClick = { confirmandoExclusao.value = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    ) {
                        Text(stringResource(R.string.meta_excluir))
                    }
                }
            }
        }
    }

    if (escolhendoPrazo.value) {
        var dataInicial: LocalDate = estado.hoje
        val prazoAtual: LocalDate? = estado.prazo
        if (prazoAtual != null) {
            dataInicial = prazoAtual
        }
        DialogoCalendario(
            dataInicial = dataInicial,
            ultimaDataPermitida = null,
            aoEscolher = { data ->
                viewModel.escolherPrazo(data)
                escolhendoPrazo.value = false
            },
            aoCancelar = { escolhendoPrazo.value = false },
        )
    }

    if (confirmandoExclusao.value) {
        DialogoConfirmacao(
            titulo = stringResource(R.string.meta_excluir_titulo),
            mensagem = stringResource(R.string.meta_excluir_mensagem, estado.descricao),
            textoConfirmar = stringResource(R.string.excluir),
            aoConfirmar = {
                confirmandoExclusao.value = false
                viewModel.excluir()
            },
            aoCancelar = { confirmandoExclusao.value = false },
        )
    }
}

@Composable
private fun CamposDaMeta(
    estado: FormularioMetaUiState,
    viewModel: FormularioMetaViewModel,
    aoEscolherPrazo: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(16.dp),
    ) {
        OutlinedTextField(
            value = estado.descricao,
            onValueChange = { texto -> viewModel.alterarDescricao(texto) },
            label = { Text(stringResource(R.string.meta_campo_descricao)) },
            placeholder = { Text(stringResource(R.string.meta_campo_descricao_exemplo)) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
        )

        if (estado.editando) {
            TextoDaFormaFixa(estado)
        } else {
            SeletorDaForma(
                formaEscolhida = estado.forma,
                temMetricas = estado.metricas.isNotEmpty(),
                aoEscolher = { forma -> viewModel.escolherForma(forma) },
            )
        }

        if (estado.forma == FormaDeAcompanhar.POR_METRICA) {
            // Na edição a métrica não muda; com uma métrica só, não há o que escolher.
            val podeTrocarMetrica: Boolean = !estado.editando && estado.metricas.size > 1
            if (podeTrocarMetrica) {
                SeletorEmMenu(
                    rotulo = stringResource(R.string.meta_campo_metrica),
                    opcoes = opcoesDeMetrica(estado.metricas),
                    idEscolhido = estado.metricaId,
                    textoSemEscolha = stringResource(R.string.meta_escolher),
                    aoEscolher = { metricaId -> viewModel.escolherMetrica(metricaId) },
                )
            }

            SeletorEmMenu(
                rotulo = stringResource(R.string.meta_campo_nivel_alvo),
                opcoes = opcoesDeNivel(estado.niveis),
                idEscolhido = estado.nivelAlvoId,
                textoSemEscolha = stringResource(R.string.meta_escolher),
                aoEscolher = { nivelId -> viewModel.escolherNivelAlvo(nivelId) },
            )
        }

        CampoDoPrazo(estado = estado, aoTocar = aoEscolherPrazo, aoRemover = { viewModel.removerPrazo() })
    }
}

/**
 * As duas formas de acompanhar, como opções de rádio. Sem nenhuma métrica criada,
 * a opção "por métrica" aparece desabilitada, com a explicação de onde criar uma.
 */
@Composable
private fun SeletorDaForma(
    formaEscolhida: FormaDeAcompanhar,
    temMetricas: Boolean,
    aoEscolher: (forma: FormaDeAcompanhar) -> Unit,
) {
    Column {
        Text(
            text = stringResource(R.string.meta_campo_forma),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OpcaoDaForma(
            texto = stringResource(R.string.meta_forma_a_mao),
            detalhe = null,
            selecionada = formaEscolhida == FormaDeAcompanhar.MARCANDO_A_MAO,
            habilitada = true,
            aoTocar = { aoEscolher(FormaDeAcompanhar.MARCANDO_A_MAO) },
        )
        var detalheDaMetrica: String? = null
        if (!temMetricas) {
            detalheDaMetrica = stringResource(R.string.meta_forma_metrica_sem_metricas)
        }
        OpcaoDaForma(
            texto = stringResource(R.string.meta_forma_metrica),
            detalhe = detalheDaMetrica,
            selecionada = formaEscolhida == FormaDeAcompanhar.POR_METRICA,
            habilitada = temMetricas,
            aoTocar = { aoEscolher(FormaDeAcompanhar.POR_METRICA) },
        )
    }
}

/** `selectable` com `Role.RadioButton`: a linha toda é tocável; o RadioButton é só o desenho. */
@Composable
private fun OpcaoDaForma(
    texto: String,
    detalhe: String?,
    selecionada: Boolean,
    habilitada: Boolean,
    aoTocar: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(selected = selecionada, enabled = habilitada, role = Role.RadioButton, onClick = aoTocar),
    ) {
        RadioButton(selected = selecionada, onClick = null, enabled = habilitada)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(text = texto, style = MaterialTheme.typography.bodyLarge)
            if (detalhe != null) {
                Text(
                    text = detalhe,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Na edição a forma não muda; só informamos qual é. */
@Composable
private fun TextoDaFormaFixa(estado: FormularioMetaUiState) {
    val texto: String
    if (estado.forma == FormaDeAcompanhar.MARCANDO_A_MAO) {
        texto = stringResource(R.string.meta_forma_fixa_a_mao)
    } else {
        texto = stringResource(R.string.meta_forma_fixa_metrica, nomeDaMetrica(estado.metricas, estado.metricaId))
    }
    Text(
        text = texto,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun nomeDaMetrica(metricas: List<Metrica>, metricaId: String?): String {
    for (metrica in metricas) {
        if (metrica.id == metricaId) {
            return metrica.nome
        }
    }
    return ""
}

/** O prazo é opcional: com data escolhida, aparece "Sem prazo" para tirá-la. */
@Composable
private fun CampoDoPrazo(estado: FormularioMetaUiState, aoTocar: () -> Unit, aoRemover: () -> Unit) {
    val prazo: LocalDate? = estado.prazo
    val texto: String
    if (prazo == null) {
        texto = stringResource(R.string.meta_escolher_data)
    } else {
        texto = formatarDataPorExtenso(prazo)
    }

    Column {
        Text(
            text = stringResource(R.string.meta_campo_prazo),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = aoTocar, modifier = Modifier.weight(1f)) {
                Text(text = texto, modifier = Modifier.weight(1f))
            }
            if (prazo != null) {
                TextButton(onClick = aoRemover) {
                    Text(stringResource(R.string.meta_remover_prazo))
                }
            }
        }
    }
}

private fun opcoesDeMetrica(metricas: List<Metrica>): List<OpcaoDoMenu> {
    val opcoes = mutableListOf<OpcaoDoMenu>()
    for (metrica in metricas) {
        opcoes.add(OpcaoDoMenu(id = metrica.id, texto = metrica.nome))
    }
    return opcoes
}

private fun opcoesDeNivel(niveis: List<NivelDaMetrica>): List<OpcaoDoMenu> {
    val opcoes = mutableListOf<OpcaoDoMenu>()
    for (nivel in niveis) {
        opcoes.add(OpcaoDoMenu(id = nivel.id, texto = nivel.nome))
    }
    return opcoes
}

/**
 * "Alunos · 11 escolhidos" e os atalhos: "Toda a turma" sempre e, nas metas por
 * métrica, um por nível. No exemplo da professora, um toque em
 * "Silábico com valor sonoro (11)" monta a lista da meta inteira.
 */
@Composable
private fun CabecalhoDosAlunos(estado: FormularioMetaUiState, viewModel: FormularioMetaViewModel) {
    val escolhidos: Int = estado.quantidadeEscolhida()
    val atalhos: List<AtalhoDeNivel> = estado.atalhos()

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = pluralStringResource(R.plurals.meta_alunos_escolhidos, escolhidos, escolhidos),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.secondary,
        )
        if (atalhos.isNotEmpty()) {
            Text(
                text = stringResource(R.string.meta_atalho_titulo),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(
                onClick = { viewModel.escolherTodaATurma() },
                label = { Text(stringResource(R.string.meta_atalho_toda_turma)) },
            )
            for (atalho in atalhos) {
                AssistChip(
                    onClick = { viewModel.escolherQuemEstaNoNivel(atalho.nivel.id) },
                    label = { Text(stringResource(R.string.meta_atalho_nivel, atalho.nivel.nome, atalho.quantidade)) },
                )
            }
        }
    }
}

/**
 * Linha inteira marcável (`toggleable`), como na chamada; o Checkbox é só o desenho.
 * O nível do aluno aparece só nas metas por métrica.
 */
@Composable
private fun LinhaDoAluno(item: AlunoParaEscolher, mostrarNivel: Boolean, aoAlternar: () -> Unit) {
    val nivel: NivelDaMetrica? = item.nivelAtual
    val textoDoNivel: String
    if (nivel == null) {
        textoDoNivel = stringResource(R.string.meta_sem_nivel)
    } else {
        textoDoNivel = nivel.nome
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = item.escolhido, role = Role.Checkbox, onValueChange = { aoAlternar() })
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Checkbox(checked = item.escolhido, onCheckedChange = null)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(text = item.aluno.nome, style = MaterialTheme.typography.bodyLarge)
            if (mostrarNivel) {
                Text(
                    text = textoDoNivel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun BotaoSalvar(habilitado: Boolean, aoSalvar: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 4.dp) {
        Button(
            onClick = aoSalvar,
            enabled = habilitado,
            modifier = Modifier
                .navigationBarsPadding()
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(stringResource(R.string.salvar))
        }
    }
}
