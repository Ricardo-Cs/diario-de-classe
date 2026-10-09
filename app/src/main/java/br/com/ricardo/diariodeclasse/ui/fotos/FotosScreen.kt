package br.com.ricardo.diariodeclasse.ui.fotos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.ricardo.diariodeclasse.R
import br.com.ricardo.diariodeclasse.ui.componentes.BarraSuperior
import br.com.ricardo.diariodeclasse.ui.componentes.BotaoFlutuante
import br.com.ricardo.diariodeclasse.ui.componentes.MensagemCentralizada
import br.com.ricardo.diariodeclasse.ui.componentes.TelaCarregando
import br.com.ricardo.diariodeclasse.ui.componentes.formatarDataComDiaDaSemana
import br.com.ricardo.diariodeclasse.ui.componentes.textoDeDataRelativa
import java.time.LocalDate

/** Todas as fotos de uma turma, por dia, do mais recente para o mais antigo. */
@Composable
fun FotosScreen(
    aoAbrirFoto: (turmaId: String, fotoId: String) -> Unit,
    aoVoltar: () -> Unit,
    viewModel: FotosViewModel = hiltViewModel(),
    adicionarFotosViewModel: AdicionarFotosViewModel = hiltViewModel(),
) {
    val estado: FotosUiState = viewModel.uiState.collectAsStateWithLifecycle().value
    val estadoDaAdicao: AdicionarFotosUiState = adicionarFotosViewModel.uiState.collectAsStateWithLifecycle().value
    val adicionarFotos: AdicionarFotos = lembrarAdicionarFotos(adicionarFotosViewModel)
    val avisos: SnackbarHostState = remember { SnackbarHostState() }
    val painelAberto: MutableState<Boolean> = remember { mutableStateOf(false) }

    MostrarMensagemDasFotos(
        mensagem = estadoDaAdicao.mensagem,
        avisos = avisos,
        aoExibir = { adicionarFotosViewModel.mensagemExibida() },
    )

    var titulo = ""
    if (estado is FotosUiState.Carregado) {
        titulo = stringResource(R.string.fotos_titulo, estado.turma.nome)
    }

    Scaffold(
        topBar = { BarraSuperior(titulo = titulo, aoVoltar = aoVoltar) },
        snackbarHost = { SnackbarHost(avisos) },
        floatingActionButton = {
            if (estado is FotosUiState.Carregado) {
                BotaoFlutuante(
                    texto = stringResource(R.string.fotos_adicionar),
                    aoClicar = { painelAberto.value = true },
                )
            }
        },
    ) { espacamentoDasBarras ->
        val modifier = Modifier.padding(espacamentoDasBarras)

        when (estado) {
            is FotosUiState.Carregando -> TelaCarregando(modifier)

            is FotosUiState.TurmaNaoEncontrada -> {
                MensagemCentralizada(stringResource(R.string.diario_sem_turma), modifier)
            }

            is FotosUiState.Carregado -> Column(modifier = modifier.fillMaxSize()) {
                if (estadoDaAdicao.salvando) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                if (estado.dias.isEmpty()) {
                    MensagemCentralizada(stringResource(R.string.fotos_vazio))
                } else {
                    GradeDeFotos(
                        estado = estado,
                        aoAbrirFoto = { fotoId -> aoAbrirFoto(estado.turma.id, fotoId) },
                    )
                }
            }
        }
    }

    if (painelAberto.value && estado is FotosUiState.Carregado) {
        FolhaAdicionarFotos(
            hoje = estado.hoje,
            aoTirarFoto = { data ->
                painelAberto.value = false
                adicionarFotos.tirarFoto(estado.turma.id, data)
            },
            aoEscolherDaGaleria = { data ->
                painelAberto.value = false
                adicionarFotos.escolherDaGaleria(estado.turma.id, data)
            },
            aoFechar = { painelAberto.value = false },
        )
    }
}

/**
 * Grade com quantas colunas couberem (3 num celular comum). O cabeçalho de cada
 * dia ocupa a linha inteira (`GridItemSpan(maxLineSpan)`), como um título de seção.
 */
@Composable
private fun GradeDeFotos(estado: FotosUiState.Carregado, aoAbrirFoto: (fotoId: String) -> Unit) {
    // O espaço extra no fim evita que o botão flutuante cubra a última linha.
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 104.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        for (dia in estado.dias) {
            item(key = "dia-${dia.data}", span = { GridItemSpan(maxLineSpan) }) {
                CabecalhoDoDia(dia = dia, hoje = estado.hoje)
            }
            items(dia.fotos, key = { foto -> foto.foto.id }) { foto ->
                MiniaturaDaFoto(
                    foto = foto,
                    descricao = descricaoDaFoto(foto, estado.hoje),
                    aoTocar = { aoAbrirFoto(foto.foto.id) },
                )
            }
        }
    }
}

/** "Hoje · 3 fotos" ou "Seg., 05/10 · 1 foto". */
@Composable
private fun CabecalhoDoDia(dia: DiaDeFotos, hoje: LocalDate) {
    var textoDoDia: String = textoDeDataRelativa(dia.data, hoje)
    if (dia.data.isBefore(hoje.minusDays(1))) {
        textoDoDia = formatarDataComDiaDaSemana(dia.data, hoje)
    }
    val quantidade: String = pluralStringResource(R.plurals.fotos_quantidade, dia.fotos.size, dia.fotos.size)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 4.dp),
    ) {
        Text(
            text = textoDoDia,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = quantidade,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Para o leitor de tela: a legenda, se houver, ou "Foto de Seg., 05/10". */
@Composable
fun descricaoDaFoto(foto: FotoNaTela, hoje: LocalDate): String {
    val legenda: String? = foto.foto.legenda
    if (legenda != null) {
        return legenda
    }
    return stringResource(R.string.foto_descricao, formatarDataComDiaDaSemana(foto.foto.data, hoje))
}
