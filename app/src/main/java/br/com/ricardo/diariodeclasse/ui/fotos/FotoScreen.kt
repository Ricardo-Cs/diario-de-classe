package br.com.ricardo.diariodeclasse.ui.fotos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.ricardo.diariodeclasse.R
import br.com.ricardo.diariodeclasse.ui.componentes.BarraSuperior
import br.com.ricardo.diariodeclasse.ui.componentes.DialogoConfirmacao
import br.com.ricardo.diariodeclasse.ui.componentes.TelaCarregando
import br.com.ricardo.diariodeclasse.ui.componentes.formatarDataComDiaDaSemana
import coil3.compose.SubcomposeAsyncImage
import java.time.LocalDate

/** O que está aberto por cima da foto. */
private sealed interface PainelDaFoto {
    data object Fechado : PainelDaFoto
    data class EditandoLegenda(val foto: FotoNaTela) : PainelDaFoto
    data class ConfirmandoExclusao(val foto: FotoNaTela) : PainelDaFoto
}

@Composable
fun FotoScreen(
    aoVoltar: () -> Unit,
    viewModel: FotoViewModel = hiltViewModel(),
) {
    val estado: FotoUiState = viewModel.uiState.collectAsStateWithLifecycle().value

    when (estado) {
        is FotoUiState.Carregando -> TelaCarregando()

        // Sem fotos para mostrar, a tela não tem o que fazer: volta para a anterior.
        is FotoUiState.SemFotos -> LaunchedEffect(Unit) { aoVoltar() }

        is FotoUiState.Carregado -> ConteudoFoto(estado, viewModel, aoVoltar)
    }
}

/**
 * `HorizontalPager` é o carrossel do Compose: uma página por foto, passando com
 * o dedo para os lados. A página atual define a foto das ações do topo.
 */
@Composable
private fun ConteudoFoto(
    estado: FotoUiState.Carregado,
    viewModel: FotoViewModel,
    aoVoltar: () -> Unit,
) {
    val paginas: PagerState = rememberPagerState(
        initialPage = estado.posicaoInicial,
        pageCount = { estado.fotos.size },
    )
    val painel: MutableState<PainelDaFoto> = remember { mutableStateOf(PainelDaFoto.Fechado) }

    // Depois de excluir a última foto da lista, a página atual pode passar do fim.
    var posicaoAtual: Int = paginas.currentPage
    if (posicaoAtual > estado.fotos.size - 1) {
        posicaoAtual = estado.fotos.size - 1
    }
    val fotoAtual: FotoNaTela = estado.fotos[posicaoAtual]

    Scaffold(
        topBar = {
            BarraSuperior(
                titulo = formatarDataComDiaDaSemana(fotoAtual.foto.data, estado.hoje),
                aoVoltar = aoVoltar,
                acoes = {
                    IconButton(onClick = { painel.value = PainelDaFoto.ConfirmandoExclusao(fotoAtual) }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_excluir),
                            contentDescription = stringResource(R.string.foto_excluir),
                        )
                    }
                },
            )
        },
    ) { espacamentoDasBarras ->
        Column(
            modifier = Modifier
                .padding(espacamentoDasBarras)
                .fillMaxSize(),
        ) {
            HorizontalPager(
                state = paginas,
                key = { posicao -> estado.fotos[posicao].foto.id },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            ) { posicao ->
                ImagemDaFoto(foto = estado.fotos[posicao], hoje = estado.hoje)
            }

            RodapeDaFoto(
                foto = fotoAtual,
                posicao = posicaoAtual,
                total = estado.fotos.size,
                aoEditarLegenda = { painel.value = PainelDaFoto.EditandoLegenda(fotoAtual) },
            )
        }
    }

    val fecharPainel = { painel.value = PainelDaFoto.Fechado }

    when (val painelAtual: PainelDaFoto = painel.value) {
        is PainelDaFoto.Fechado -> {}

        is PainelDaFoto.EditandoLegenda -> FolhaLegenda(
            legendaInicial = painelAtual.foto.foto.legenda,
            aoSalvar = { legenda ->
                viewModel.editarLegenda(painelAtual.foto.foto.id, legenda)
                fecharPainel()
            },
            aoFechar = fecharPainel,
        )

        // Sem "Desfazer": a professora confirma antes, porque foto excluída não volta.
        is PainelDaFoto.ConfirmandoExclusao -> DialogoConfirmacao(
            titulo = stringResource(R.string.foto_excluir_confirmar_titulo),
            mensagem = stringResource(R.string.foto_excluir_confirmar_mensagem),
            textoConfirmar = stringResource(R.string.excluir),
            aoConfirmar = {
                viewModel.excluir(painelAtual.foto.foto.id)
                fecharPainel()
            },
            aoCancelar = fecharPainel,
        )
    }
}

/**
 * A foto inteira, sem cortes (`ContentScale.Fit`). `SubcomposeAsyncImage` permite
 * mostrar um texto no lugar da imagem quando o arquivo não existe, o que acontece
 * quando o banco veio do backup automático do Android, que não leva as fotos.
 */
@Composable
private fun ImagemDaFoto(foto: FotoNaTela, hoje: LocalDate) {
    SubcomposeAsyncImage(
        model = foto.arquivo,
        contentDescription = descricaoDaFoto(foto, hoje),
        contentScale = ContentScale.Fit,
        loading = { TelaCarregando() },
        error = { AvisoDeFotoAusente() },
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun AvisoDeFotoAusente() {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(32.dp)) {
        Text(
            text = stringResource(R.string.foto_nao_encontrada),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Legenda (ou o botão para adicionar uma) e "3 de 12". */
@Composable
private fun RodapeDaFoto(
    foto: FotoNaTela,
    posicao: Int,
    total: Int,
    aoEditarLegenda: () -> Unit,
) {
    val legenda: String? = foto.foto.legenda

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        if (legenda != null) {
            Text(text = legenda, style = MaterialTheme.typography.bodyLarge)
        }
        TextButton(onClick = aoEditarLegenda) {
            if (legenda == null) {
                Text(stringResource(R.string.foto_adicionar_legenda))
            } else {
                Text(stringResource(R.string.foto_editar_legenda))
            }
        }
        Text(
            text = stringResource(R.string.foto_posicao, posicao + 1, total),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
