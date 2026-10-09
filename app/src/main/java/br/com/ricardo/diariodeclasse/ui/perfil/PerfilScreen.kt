package br.com.ricardo.diariodeclasse.ui.perfil

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.ricardo.diariodeclasse.R
import br.com.ricardo.diariodeclasse.ui.componentes.BarraSuperior

/** Qual painel está aberto: nenhum, o da foto ou o de editar nome e escola. */
private enum class PainelDoPerfil {
    FECHADO,
    FOTO,
    EDITAR,
}

/**
 * Aba Perfil, no estilo do Instagram: foto, nome, escola e os números do diário.
 * A engrenagem do topo leva às configurações (backup dos dados).
 */
@Composable
fun PerfilScreen(
    aoAbrirConfiguracoes: () -> Unit,
    viewModel: PerfilViewModel = hiltViewModel(),
) {
    val estado: PerfilUiState = viewModel.uiState.collectAsStateWithLifecycle().value
    val avisos: SnackbarHostState = remember { SnackbarHostState() }
    val painel: MutableState<PainelDoPerfil> = remember { mutableStateOf(PainelDoPerfil.FECHADO) }

    /*
     * Câmera (`TakePicture`) e galeria (`PickVisualMedia`) como nas fotos do dia:
     * nenhuma das duas pede permissão (ver `AdicionarFotos`). Aqui a galeria
     * escolhe uma foto só.
     */
    val camera: ManagedActivityResultLauncher<Uri, Boolean> = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { sucesso: Boolean ->
        viewModel.fotoTirada(sucesso)
    }
    val galeria: ManagedActivityResultLauncher<PickVisualMediaRequest, Uri?> = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { escolhida: Uri? ->
        viewModel.fotoEscolhida(escolhida)
    }

    fun tirarFoto() {
        painel.value = PainelDoPerfil.FECHADO
        val destino: Uri = viewModel.prepararCamera()
        try {
            camera.launch(destino)
        } catch (erro: ActivityNotFoundException) {
            viewModel.cameraIndisponivel()
        }
    }

    fun escolherDaGaleria() {
        painel.value = PainelDoPerfil.FECHADO
        galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    MostrarMensagemDoPerfil(mensagem = estado.mensagem, avisos = avisos, aoExibir = { viewModel.mensagemExibida() })

    Scaffold(
        topBar = {
            BarraSuperior(
                titulo = stringResource(R.string.aba_perfil),
                acoes = {
                    IconButton(onClick = aoAbrirConfiguracoes) {
                        Icon(
                            painter = painterResource(R.drawable.ic_configuracoes),
                            contentDescription = stringResource(R.string.configuracoes_titulo),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(avisos) },
    ) { espacamentoDasBarras ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .padding(espacamentoDasBarras)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 24.dp),
        ) {
            FotoQueTroca(estado = estado, aoTocar = { painel.value = PainelDoPerfil.FOTO })
            NomeEEscola(estado)
            OutlinedButton(onClick = { painel.value = PainelDoPerfil.EDITAR }) {
                if (estado.nome.isBlank()) {
                    Text(stringResource(R.string.perfil_preencher))
                } else {
                    Text(stringResource(R.string.perfil_editar))
                }
            }
            ResumoDoDiario(estado)
        }
    }

    val fecharPainel = { painel.value = PainelDoPerfil.FECHADO }

    when (painel.value) {
        PainelDoPerfil.FECHADO -> {}

        PainelDoPerfil.FOTO -> FolhaFotoDoPerfil(
            temFoto = estado.temFoto,
            aoTirarFoto = { tirarFoto() },
            aoEscolherDaGaleria = { escolherDaGaleria() },
            aoRemover = {
                viewModel.removerFoto()
                fecharPainel()
            },
            aoFechar = fecharPainel,
        )

        PainelDoPerfil.EDITAR -> FolhaEditarPerfil(
            nomeInicial = estado.nome,
            escolaInicial = estado.escola ?: "",
            aoSalvar = { nome, escola ->
                viewModel.salvarNomeEEscola(nome, escola)
                fecharPainel()
            },
            aoFechar = fecharPainel,
        )
    }
}

/** A foto grande, com um selo de câmera que indica que dá para trocar. */
@Composable
private fun FotoQueTroca(estado: PerfilUiState, aoTocar: () -> Unit) {
    val descricao: String = stringResource(R.string.perfil_trocar_foto)

    Box(modifier = Modifier.clickable(onClickLabel = descricao, onClick = aoTocar)) {
        FotoDoPerfil(arquivo = estado.arquivoDaFoto, nome = estado.nome, tamanho = 112.dp)

        if (estado.salvandoFoto) {
            CircularProgressIndicator(modifier = Modifier.size(112.dp))
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondary),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_camera),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun NomeEEscola(estado: PerfilUiState) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (estado.nome.isBlank()) {
            Text(
                text = stringResource(R.string.perfil_sem_nome),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.perfil_convite),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            return
        }

        Text(
            text = estado.nome,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        val escola: String? = estado.escola
        if (escola != null) {
            Text(
                text = escola,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** "2 turmas · 47 alunos · 120 anotações", em três colunas como no Instagram. */
@Composable
private fun ResumoDoDiario(estado: PerfilUiState) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    ) {
        NumeroDoResumo(
            valor = estado.resumo.turmas,
            rotulo = pluralStringResource(R.plurals.perfil_turmas, estado.resumo.turmas),
        )
        NumeroDoResumo(
            valor = estado.resumo.alunos,
            rotulo = pluralStringResource(R.plurals.perfil_alunos, estado.resumo.alunos),
        )
        NumeroDoResumo(
            valor = estado.resumo.anotacoes,
            rotulo = pluralStringResource(R.plurals.perfil_anotacoes, estado.resumo.anotacoes),
        )
    }
}

@Composable
private fun NumeroDoResumo(valor: Int, rotulo: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = valor.toString(), style = MaterialTheme.typography.titleLarge)
        Text(
            text = rotulo,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Mostra o aviso no rodapé quando o ViewModel publica uma mensagem nova (ver `MostrarMensagem` das Configurações). */
@Composable
private fun MostrarMensagemDoPerfil(mensagem: MensagemDoPerfil?, avisos: SnackbarHostState, aoExibir: () -> Unit) {
    val texto: String? = textoDaMensagem(mensagem)

    LaunchedEffect(mensagem) {
        if (texto != null) {
            avisos.showSnackbar(texto)
            aoExibir()
        }
    }
}

@Composable
private fun textoDaMensagem(mensagem: MensagemDoPerfil?): String? {
    if (mensagem == null) {
        return null
    }
    val idDoTexto: Int = when (mensagem) {
        MensagemDoPerfil.FOTO_NAO_LIDA -> R.string.perfil_mensagem_foto_nao_lida
        MensagemDoPerfil.SEM_CAMERA -> R.string.fotos_mensagem_sem_camera
    }
    return stringResource(idDoTexto)
}
