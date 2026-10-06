package br.com.ricardo.diariodeclasse.ui.chamada

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.ricardo.diariodeclasse.R
import br.com.ricardo.diariodeclasse.ui.componentes.BarraSuperior
import br.com.ricardo.diariodeclasse.ui.componentes.DialogoConfirmacao
import br.com.ricardo.diariodeclasse.ui.componentes.MensagemCentralizada
import br.com.ricardo.diariodeclasse.ui.componentes.TelaCarregando
import br.com.ricardo.diariodeclasse.ui.componentes.formatarDataPorExtenso

@Composable
fun ChamadaScreen(
    aoVoltar: () -> Unit,
    viewModel: ChamadaViewModel = hiltViewModel(),
) {
    val estado: ChamadaUiState = viewModel.uiState.collectAsStateWithLifecycle().value

    when (estado) {
        is ChamadaUiState.Carregando -> TelaCarregando()

        is ChamadaUiState.TurmaNaoEncontrada -> {
            LaunchedEffect(Unit) {
                aoVoltar()
            }
        }

        is ChamadaUiState.Carregado -> {
            val chamadaConcluida: Boolean = estado.etapa is EtapaDaChamada.Concluida
            LaunchedEffect(chamadaConcluida) {
                if (chamadaConcluida) {
                    aoVoltar()
                }
            }
            ConteudoChamada(estado = estado, viewModel = viewModel, aoVoltar = aoVoltar)
        }
    }
}

@Composable
private fun ConteudoChamada(
    estado: ChamadaUiState.Carregado,
    viewModel: ChamadaViewModel,
    aoVoltar: () -> Unit,
) {
    val titulo: String = if (estado.editando) {
        stringResource(R.string.chamada_titulo_edicao)
    } else {
        stringResource(R.string.chamada_titulo)
    }
    val temAlunos: Boolean = estado.alunos.isNotEmpty()

    // Aluno cuja observação está sendo escrita; `null` = painel fechado.
    val alunoEditandoObservacao: MutableState<AlunoNaChamada?> = remember { mutableStateOf(null) }
    val confirmandoDescarte: MutableState<Boolean> = remember { mutableStateOf(false) }

    // Depois de salvar, a chamada já está gravada e voltar não perde nada.
    val perderiaAlteracoes: Boolean = estado.etapa is EtapaDaChamada.Marcando &&
        !estado.salvando &&
        estado.temAlteracoesNaoSalvas()

    fun tentarVoltar() {
        if (perderiaAlteracoes) {
            confirmandoDescarte.value = true
        } else {
            aoVoltar()
        }
    }

    // Intercepta o "voltar" do sistema (gesto ou botão) só enquanto há algo a perder;
    // com `enabled = false`, o voltar segue o caminho normal da navegação.
    BackHandler(enabled = perderiaAlteracoes) {
        tentarVoltar()
    }

    Scaffold(
        topBar = { BarraSuperior(titulo = titulo, aoVoltar = { tentarVoltar() }) },
        bottomBar = {
            if (temAlunos) {
                BotaoSalvar(salvando = estado.salvando, aoSalvar = { viewModel.salvar() })
            }
        },
    ) { espacamentoDasBarras ->
        val modifier = Modifier.padding(espacamentoDasBarras)

        if (!temAlunos) {
            MensagemCentralizada(stringResource(R.string.chamada_sem_alunos), modifier)
        } else {
            LazyColumn(modifier = modifier.fillMaxSize()) {
                item {
                    CabecalhoDaChamada(estado)
                    HorizontalDivider()
                }
                items(estado.alunos, key = { linha -> linha.aluno.id }) { linha ->
                    LinhaDoAluno(
                        linha = linha,
                        aoTocar = { viewModel.alternarPresenca(linha.aluno.id) },
                        aoTocarObservacao = { alunoEditandoObservacao.value = linha },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }

    val etapa: EtapaDaChamada = estado.etapa
    if (etapa is EtapaDaChamada.OferecendoPendencias) {
        FolhaPendenciasDaFalta(
            dataDaChamada = estado.data,
            etapa = etapa,
            aoAdicionar = { descricao, registroPresencaIds, dataLembrete ->
                viewModel.adicionarAtividadeParaAusentes(descricao, registroPresencaIds, dataLembrete)
            },
            aoConcluir = { viewModel.concluir() },
        )
    }

    val linhaEmEdicao: AlunoNaChamada? = alunoEditandoObservacao.value
    if (linhaEmEdicao != null) {
        FolhaObservacao(
            nomeDoAluno = linhaEmEdicao.aluno.nome,
            observacaoAtual = linhaEmEdicao.observacao,
            aoConcluir = { observacao ->
                viewModel.alterarObservacao(linhaEmEdicao.aluno.id, observacao)
                alunoEditandoObservacao.value = null
            },
            aoFechar = { alunoEditandoObservacao.value = null },
        )
    }

    if (confirmandoDescarte.value) {
        DialogoConfirmacao(
            titulo = stringResource(R.string.chamada_descartar_titulo),
            mensagem = stringResource(R.string.chamada_descartar_mensagem),
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
 * Turma, data e um resumo "3 alunos · 2 ausentes". A instrução só aparece
 * numa chamada nova e ainda sem faltas, enquanto a professora precisa de orientação.
 */
@Composable
private fun CabecalhoDaChamada(estado: ChamadaUiState.Carregado) {
    val totalDeAlunos: Int = estado.alunos.size
    val ausentes: Int = estado.quantidadeDeAusentes()
    val mostrarInstrucao: Boolean = !estado.editando && ausentes == 0

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(text = estado.turma.nome, style = MaterialTheme.typography.titleMedium)
        Text(
            text = formatarDataPorExtenso(estado.data),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Text(
                text = pluralStringResource(R.plurals.chamada_alunos, totalDeAlunos, totalDeAlunos),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            ResumoDeAusentes(ausentes)
        }

        if (mostrarInstrucao) {
            Text(
                text = stringResource(R.string.chamada_instrucao),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun ResumoDeAusentes(ausentes: Int) {
    if (ausentes == 0) {
        Text(
            text = stringResource(R.string.chamada_nenhum_ausente),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        Text(
            text = pluralStringResource(R.plurals.chamada_ausentes, ausentes, ausentes),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

/**
 * Tocar na linha alterna presente/faltou. Quem faltou ganha um fundo vermelho bem
 * suave, uma borda fina à esquerda e, abaixo do nome, o atalho para a observação.
 *
 * `IntrinsicSize.Min` faz a linha ter a altura do seu conteúdo, para que a borda
 * (`fillMaxHeight`) acompanhe a altura do texto em vez de ocupar a tela inteira.
 */
@Composable
private fun LinhaDoAluno(
    linha: AlunoNaChamada,
    aoTocar: () -> Unit,
    aoTocarObservacao: () -> Unit,
) {
    val corDaBorda: Color
    val corDeFundo: Color
    if (linha.ausente) {
        corDaBorda = MaterialTheme.colorScheme.error
        corDeFundo = MaterialTheme.colorScheme.error.copy(alpha = 0.06f)
    } else {
        corDaBorda = Color.Transparent
        corDeFundo = Color.Transparent
    }

    Row(
        modifier = Modifier
            .height(IntrinsicSize.Min)
            .fillMaxWidth()
            .background(corDeFundo)
            .clickable(onClick = aoTocar),
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(corDaBorda),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 13.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = linha.aluno.nome, style = MaterialTheme.typography.bodyLarge)
                if (linha.ausente) {
                    AtalhoDaObservacao(observacao = linha.observacao, aoTocar = aoTocarObservacao)
                }
            }
            IndicadorDeSituacao(ausente = linha.ausente)
        }
    }
}

/**
 * Mostra a prévia da observação ou, se ainda não houver, o convite para adicionar.
 *
 * O atalho fica dentro da linha que alterna a presença: com uma área de toque
 * pequena, um toque um pouco fora desmarcaria a falta. Por isso a altura mínima
 * de 48dp (o tamanho de toque recomendado pelo Android).
 */
@Composable
private fun AtalhoDaObservacao(observacao: String, aoTocar: () -> Unit) {
    val temObservacao: Boolean = observacao.isNotBlank()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clickable(onClick = aoTocar)
            .padding(end = 8.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_anotacao),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(4.dp))
        if (temObservacao) {
            Text(
                text = observacao,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            Text(
                text = stringResource(R.string.chamada_adicionar_observacao),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/** Ícone pequeno + "Presente"/"Faltou", com menos destaque que o nome. */
@Composable
private fun IndicadorDeSituacao(ausente: Boolean) {
    val icone: Int
    val texto: String
    val cor: Color
    if (ausente) {
        icone = R.drawable.ic_ausente
        texto = stringResource(R.string.chamada_faltou)
        cor = MaterialTheme.colorScheme.error
    } else {
        icone = R.drawable.ic_presente
        texto = stringResource(R.string.chamada_presente)
        cor = MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = texto, style = MaterialTheme.typography.labelMedium, color = cor)
        Spacer(Modifier.width(6.dp))
        Icon(
            painter = painterResource(icone),
            contentDescription = null,
            tint = cor,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** `navigationBarsPadding` afasta o botão da barra de gestos do sistema. */
@Composable
private fun BotaoSalvar(salvando: Boolean, aoSalvar: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 4.dp) {
        Button(
            onClick = aoSalvar,
            enabled = !salvando,
            modifier = Modifier
                .navigationBarsPadding()
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(stringResource(R.string.chamada_salvar))
        }
    }
}
