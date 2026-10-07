package br.com.ricardo.diariodeclasse.ui.metricas

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.ricardo.diariodeclasse.R
import br.com.ricardo.diariodeclasse.data.local.entity.NivelDaMetrica
import br.com.ricardo.diariodeclasse.ui.componentes.BarraSuperior
import br.com.ricardo.diariodeclasse.ui.componentes.DialogoConfirmacao
import br.com.ricardo.diariodeclasse.ui.componentes.MensagemCentralizada
import br.com.ricardo.diariodeclasse.ui.componentes.TelaCarregando
import br.com.ricardo.diariodeclasse.ui.componentes.formatarDataPorExtenso

@Composable
fun SondagemScreen(
    aoVoltar: () -> Unit,
    viewModel: SondagemViewModel = hiltViewModel(),
) {
    val estado: SondagemUiState = viewModel.uiState.collectAsStateWithLifecycle().value

    when (estado) {
        is SondagemUiState.Carregando -> TelaCarregando()

        is SondagemUiState.MetricaNaoEncontrada -> {
            LaunchedEffect(Unit) {
                aoVoltar()
            }
        }

        is SondagemUiState.Carregado -> {
            LaunchedEffect(estado.salva) {
                if (estado.salva) {
                    aoVoltar()
                }
            }
            ConteudoSondagem(estado = estado, viewModel = viewModel, aoVoltar = aoVoltar)
        }
    }
}

@Composable
private fun ConteudoSondagem(
    estado: SondagemUiState.Carregado,
    viewModel: SondagemViewModel,
    aoVoltar: () -> Unit,
) {
    val titulo: String
    if (estado.editando) {
        titulo = stringResource(R.string.sondagem_titulo_edicao)
    } else {
        titulo = stringResource(R.string.sondagem_titulo)
    }
    val temAlunos: Boolean = estado.alunos.isNotEmpty()

    // Aluno cujo nível está sendo escolhido; `null` = painel fechado.
    val alunoEscolhendoNivel: MutableState<AlunoNaSondagem?> = remember { mutableStateOf(null) }
    val confirmandoDescarte: MutableState<Boolean> = remember { mutableStateOf(false) }

    val perderiaAlteracoes: Boolean = !estado.salvando && !estado.salva && estado.temAlteracoesNaoSalvas()

    fun tentarVoltar() {
        if (perderiaAlteracoes) {
            confirmandoDescarte.value = true
        } else {
            aoVoltar()
        }
    }

    BackHandler(enabled = perderiaAlteracoes) {
        tentarVoltar()
    }

    Scaffold(
        topBar = { BarraSuperior(titulo = titulo, aoVoltar = { tentarVoltar() }) },
        bottomBar = {
            if (temAlunos) {
                BotaoSalvar(
                    mudancas = estado.quantidadeDeMudancas(),
                    salvando = estado.salvando,
                    aoSalvar = { viewModel.salvar() },
                )
            }
        },
    ) { espacamentoDasBarras ->
        val modifier = Modifier.padding(espacamentoDasBarras)

        if (!temAlunos) {
            MensagemCentralizada(stringResource(R.string.metrica_sem_alunos), modifier)
        } else {
            LazyColumn(modifier = modifier.fillMaxSize()) {
                item {
                    CabecalhoDaSondagem(estado)
                    HorizontalDivider()
                }
                items(estado.alunos, key = { linha -> linha.aluno.id }) { linha ->
                    LinhaDoAluno(
                        linha = linha,
                        niveis = estado.niveis,
                        aoTocar = { alunoEscolhendoNivel.value = linha },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }

    val linhaEmEscolha: AlunoNaSondagem? = alunoEscolhendoNivel.value
    if (linhaEmEscolha != null) {
        FolhaEscolhaDeNivel(
            nomeDoAluno = linhaEmEscolha.aluno.nome,
            niveis = estado.niveis,
            nivelEscolhidoId = linhaEmEscolha.nivelId,
            aoEscolher = { nivelId ->
                viewModel.escolherNivel(linhaEmEscolha.aluno.id, nivelId)
                alunoEscolhendoNivel.value = null
            },
            aoFechar = { alunoEscolhendoNivel.value = null },
        )
    }

    if (confirmandoDescarte.value) {
        DialogoConfirmacao(
            titulo = stringResource(R.string.sondagem_descartar_titulo),
            mensagem = stringResource(R.string.sondagem_descartar_mensagem),
            textoConfirmar = stringResource(R.string.chamada_descartar),
            aoConfirmar = {
                confirmandoDescarte.value = false
                aoVoltar()
            },
            aoCancelar = { confirmandoDescarte.value = false },
        )
    }
}

/**
 * Métrica, data e "3 mudaram de nível". A instrução aparece só numa sondagem nova
 * que tem uma anterior para trazer os níveis; na primeira, todos começam sem nível.
 */
@Composable
private fun CabecalhoDaSondagem(estado: SondagemUiState.Carregado) {
    val mudancas: Int = estado.quantidadeDeMudancas()
    val resumo: String
    if (mudancas == 0) {
        resumo = stringResource(R.string.sondagem_nenhuma_mudanca)
    } else {
        resumo = pluralStringResource(R.plurals.sondagem_mudancas, mudancas, mudancas)
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(text = estado.metrica.nome, style = MaterialTheme.typography.titleMedium)
        Text(
            text = formatarDataPorExtenso(estado.data),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Text(
                text = pluralStringResource(R.plurals.chamada_alunos, estado.alunos.size, estado.alunos.size),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = resumo,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        if (!estado.editando && estado.temSondagemAnterior()) {
            Text(
                text = stringResource(R.string.sondagem_instrucao),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/**
 * Nome à esquerda e nível à direita. Quem mudou de nível ganha um fundo suave e,
 * abaixo do nome, o nível anterior ("antes: Silábico sem valor sonoro"), para a
 * professora conferir as mudanças antes de salvar.
 */
@Composable
private fun LinhaDoAluno(
    linha: AlunoNaSondagem,
    niveis: List<NivelDaMetrica>,
    aoTocar: () -> Unit,
) {
    val mudou: Boolean = linha.mudouDeNivel()
    val nivelAtual: NivelDaMetrica? = buscarNivel(niveis, linha.nivelId)
    val nivelAnterior: NivelDaMetrica? = buscarNivel(niveis, linha.nivelAnteriorId)

    val corDeFundo: Color
    if (mudou) {
        corDeFundo = MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f)
    } else {
        corDeFundo = Color.Transparent
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(corDeFundo)
            .clickable(onClick = aoTocar)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = linha.aluno.nome, style = MaterialTheme.typography.bodyLarge)
            if (mudou && nivelAnterior != null) {
                Text(
                    text = stringResource(R.string.sondagem_antes, nivelAnterior.nome),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        TextoDoNivel(nivel = nivelAtual, destacado = mudou, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun TextoDoNivel(nivel: NivelDaMetrica?, destacado: Boolean, modifier: Modifier) {
    if (nivel == null) {
        Text(
            text = stringResource(R.string.sondagem_nao_avaliado),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = modifier,
        )
        return
    }

    val cor: Color
    if (destacado) {
        cor = MaterialTheme.colorScheme.secondary
    } else {
        cor = MaterialTheme.colorScheme.onSurface
    }
    Text(
        text = nivel.nome,
        style = MaterialTheme.typography.labelLarge,
        color = cor,
        textAlign = TextAlign.End,
        modifier = modifier,
    )
}

/** Como na chamada, o botão repete o resumo ("Salvar sondagem · 3 mudanças"). */
@Composable
private fun BotaoSalvar(mudancas: Int, salvando: Boolean, aoSalvar: () -> Unit) {
    val texto: String
    if (mudancas == 0) {
        texto = stringResource(R.string.sondagem_salvar)
    } else {
        texto = pluralStringResource(R.plurals.sondagem_salvar_com_mudancas, mudancas, mudancas)
    }

    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 4.dp) {
        Button(
            onClick = aoSalvar,
            enabled = !salvando,
            modifier = Modifier
                .navigationBarsPadding()
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(texto)
        }
    }
}
