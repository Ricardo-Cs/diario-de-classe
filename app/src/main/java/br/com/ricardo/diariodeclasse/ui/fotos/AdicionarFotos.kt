package br.com.ricardo.diariodeclasse.ui.fotos

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import br.com.ricardo.diariodeclasse.R
import java.time.LocalDate

/** Quantas fotos a professora pode escolher de uma vez na galeria. */
private const val MAXIMO_DE_FOTOS_DA_GALERIA = 30

/**
 * Abre a câmera ou a galeria e entrega o resultado ao [viewModel].
 *
 * - Câmera (`TakePicture`): abre o app de câmera do celular, que grava a foto no
 *   endereço que entregamos. Por usar o app de câmera, este app não precisa da
 *   permissão de câmera.
 * - Galeria (`PickMultipleVisualMedia`): o seletor de fotos do Android. Também não
 *   pede permissão: o app só enxerga as fotos que a professora escolher.
 */
class AdicionarFotos(
    private val viewModel: AdicionarFotosViewModel,
    private val camera: ManagedActivityResultLauncher<Uri, Boolean>,
    private val galeria: ManagedActivityResultLauncher<PickVisualMediaRequest, List<Uri>>,
) {
    fun tirarFoto(turmaId: String, data: LocalDate) {
        val destino: Uri = viewModel.prepararCamera(turmaId, data)
        try {
            camera.launch(destino)
        } catch (erro: ActivityNotFoundException) {
            viewModel.cameraIndisponivel()
        }
    }

    fun escolherDaGaleria(turmaId: String, data: LocalDate) {
        viewModel.prepararGaleria(turmaId, data)
        val somenteImagens = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        galeria.launch(somenteImagens)
    }
}

/**
 * Os "launchers" precisam ser criados durante a composição (como os hooks do
 * React), por isso esta função `@Composable` monta o [AdicionarFotos].
 */
@Composable
fun lembrarAdicionarFotos(viewModel: AdicionarFotosViewModel): AdicionarFotos {
    val camera: ManagedActivityResultLauncher<Uri, Boolean> = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { sucesso: Boolean ->
        viewModel.fotoTirada(sucesso)
    }
    val galeria: ManagedActivityResultLauncher<PickVisualMediaRequest, List<Uri>> = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(MAXIMO_DE_FOTOS_DA_GALERIA),
    ) { escolhidas: List<Uri> ->
        viewModel.adicionarDaGaleria(escolhidas)
    }
    return AdicionarFotos(viewModel, camera, galeria)
}

/** Mostra no rodapé o resultado de adicionar fotos (ver `MostrarMensagem` da tela Mais). */
@Composable
fun MostrarMensagemDasFotos(mensagem: MensagemDasFotos?, avisos: SnackbarHostState, aoExibir: () -> Unit) {
    val texto: String? = textoDaMensagem(mensagem)

    LaunchedEffect(mensagem) {
        if (texto != null) {
            avisos.showSnackbar(texto)
            aoExibir()
        }
    }
}

@Composable
private fun textoDaMensagem(mensagem: MensagemDasFotos?): String? {
    return when (mensagem) {
        null -> null
        is MensagemDasFotos.Adicionadas -> pluralStringResource(
            R.plurals.fotos_mensagem_adicionadas,
            mensagem.quantidade,
            mensagem.quantidade,
        )
        is MensagemDasFotos.Falharam -> pluralStringResource(
            R.plurals.fotos_mensagem_falharam,
            mensagem.quantidade,
            mensagem.quantidade,
        )
        is MensagemDasFotos.SemCamera -> stringResource(R.string.fotos_mensagem_sem_camera)
    }
}
