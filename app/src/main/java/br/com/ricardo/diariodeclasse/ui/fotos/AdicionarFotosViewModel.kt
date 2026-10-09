package br.com.ricardo.diariodeclasse.ui.fotos

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.ricardo.diariodeclasse.data.fotos.ArquivoDaCamera
import br.com.ricardo.diariodeclasse.data.repository.FotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.LocalDate
import javax.inject.Inject

/** Avisos mostrados uma vez no rodapé da tela depois de adicionar fotos. */
sealed interface MensagemDasFotos {
    data class Adicionadas(val quantidade: Int) : MensagemDasFotos
    data class Falharam(val quantidade: Int) : MensagemDasFotos
    data object SemCamera : MensagemDasFotos
}

data class AdicionarFotosUiState(
    /** Reduzindo e gravando as fotos: a tela mostra uma barra de progresso. */
    val salvando: Boolean = false,
    val mensagem: MensagemDasFotos? = null,
)

/**
 * Adiciona fotos pela câmera ou pela galeria. Usado pela aba Acompanhamento e pela tela
 * de fotos da turma (ver `lembrarAdicionarFotos`).
 *
 * A câmera e a galeria são outros apps: enquanto estão abertos, o Android pode
 * fechar este app para liberar memória. Por isso a turma, o dia e o arquivo da
 * câmera ficam no [SavedStateHandle], que sobrevive a isso (como o
 * `onSaveInstanceState`), e não só em variáveis do ViewModel.
 */
@HiltViewModel
class AdicionarFotosViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val fotoRepository: FotoRepository,
) : ViewModel() {

    private val estado = MutableStateFlow(AdicionarFotosUiState())
    val uiState: StateFlow<AdicionarFotosUiState> = estado

    /** Guarda para onde vão as fotos e devolve o endereço onde a câmera deve gravar. */
    fun prepararCamera(turmaId: String, data: LocalDate): Uri {
        val arquivo: ArquivoDaCamera = fotoRepository.prepararCamera()
        guardarDestino(turmaId, data)
        savedStateHandle[CHAVE_ARQUIVO_DA_CAMERA] = arquivo.nomeTemporario
        return arquivo.uri
    }

    /** [sucesso] é `false` quando a professora fecha a câmera sem tirar a foto. */
    fun fotoTirada(sucesso: Boolean) {
        val nomeTemporario: String = savedStateHandle[CHAVE_ARQUIVO_DA_CAMERA] ?: return
        val destino: DestinoDasFotos = destinoGuardado() ?: return
        savedStateHandle.remove<String>(CHAVE_ARQUIVO_DA_CAMERA)

        if (!sucesso) {
            viewModelScope.launch {
                fotoRepository.descartarCamera(nomeTemporario)
            }
            return
        }

        estado.value = estado.value.copy(salvando = true)
        viewModelScope.launch {
            var mensagem: MensagemDasFotos = MensagemDasFotos.Adicionadas(1)
            try {
                fotoRepository.adicionarDaCamera(destino.turmaId, destino.data, nomeTemporario)
            } catch (erro: IOException) {
                mensagem = MensagemDasFotos.Falharam(1)
            }
            estado.value = AdicionarFotosUiState(salvando = false, mensagem = mensagem)
        }
    }

    /** O celular não tem app de câmera (raro, mas acontece em alguns tablets). */
    fun cameraIndisponivel() {
        val nomeTemporario: String? = savedStateHandle[CHAVE_ARQUIVO_DA_CAMERA]
        savedStateHandle.remove<String>(CHAVE_ARQUIVO_DA_CAMERA)
        if (nomeTemporario != null) {
            viewModelScope.launch {
                fotoRepository.descartarCamera(nomeTemporario)
            }
        }
        estado.value = estado.value.copy(mensagem = MensagemDasFotos.SemCamera)
    }

    fun prepararGaleria(turmaId: String, data: LocalDate) {
        guardarDestino(turmaId, data)
    }

    /**
     * Uma foto com problema (ex.: formato que o celular não lê) não impede as
     * outras: cada uma é salva separadamente, e o aviso diz quantas falharam.
     */
    fun adicionarDaGaleria(escolhidas: List<Uri>) {
        val destino: DestinoDasFotos = destinoGuardado() ?: return
        if (escolhidas.isEmpty()) {
            return
        }
        estado.value = estado.value.copy(salvando = true)

        viewModelScope.launch {
            var falharam = 0
            for (origem in escolhidas) {
                try {
                    fotoRepository.adicionar(destino.turmaId, destino.data, origem)
                } catch (erro: IOException) {
                    falharam = falharam + 1
                } catch (erro: SecurityException) {
                    falharam = falharam + 1
                }
            }

            val mensagem: MensagemDasFotos
            if (falharam > 0) {
                mensagem = MensagemDasFotos.Falharam(falharam)
            } else {
                mensagem = MensagemDasFotos.Adicionadas(escolhidas.size)
            }
            estado.value = AdicionarFotosUiState(salvando = false, mensagem = mensagem)
        }
    }

    fun mensagemExibida() {
        estado.value = estado.value.copy(mensagem = null)
    }

    /** O `SavedStateHandle` guarda tipos simples; a data vai como texto ("2026-10-09"). */
    private fun guardarDestino(turmaId: String, data: LocalDate) {
        savedStateHandle[CHAVE_TURMA] = turmaId
        savedStateHandle[CHAVE_DATA] = data.toString()
    }

    private fun destinoGuardado(): DestinoDasFotos? {
        val turmaId: String? = savedStateHandle[CHAVE_TURMA]
        val data: String? = savedStateHandle[CHAVE_DATA]
        if (turmaId == null || data == null) {
            return null
        }
        return DestinoDasFotos(turmaId, LocalDate.parse(data))
    }

    private data class DestinoDasFotos(val turmaId: String, val data: LocalDate)

    companion object {
        private const val CHAVE_TURMA = "fotos_turma"
        private const val CHAVE_DATA = "fotos_data"
        private const val CHAVE_ARQUIVO_DA_CAMERA = "fotos_arquivo_da_camera"
    }
}
