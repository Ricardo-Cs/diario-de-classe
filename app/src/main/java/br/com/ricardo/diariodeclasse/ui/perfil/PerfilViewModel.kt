package br.com.ricardo.diariodeclasse.ui.perfil

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.ricardo.diariodeclasse.data.fotos.ArquivoDaCamera
import br.com.ricardo.diariodeclasse.data.local.entity.Perfil
import br.com.ricardo.diariodeclasse.data.local.entity.ResumoDoPerfil
import br.com.ricardo.diariodeclasse.data.repository.PerfilRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import javax.inject.Inject

/** Avisos de resultado da troca de foto, mostrados uma vez no rodapé. */
enum class MensagemDoPerfil {
    FOTO_NAO_LIDA,
    SEM_CAMERA,
}

data class PerfilUiState(
    /** Vazio enquanto ela não preencheu o nome. */
    val nome: String = "",
    val escola: String? = null,
    val arquivoDaFoto: File? = null,
    val temFoto: Boolean = false,
    val resumo: ResumoDoPerfil = ResumoDoPerfil(turmas = 0, alunos = 0, anotacoes = 0),
    val salvandoFoto: Boolean = false,
    val mensagem: MensagemDoPerfil? = null,
)

/** O que muda pelas ações da tela; o resto vem do banco. */
private data class EstadoDaFoto(
    val salvando: Boolean = false,
    val mensagem: MensagemDoPerfil? = null,
)

/**
 * Aba Perfil e foto da barra inferior. A câmera é outro app: enquanto está
 * aberta, o Android pode fechar este app, por isso o arquivo da câmera fica no
 * [SavedStateHandle] (ver `AdicionarFotosViewModel`).
 */
@HiltViewModel
class PerfilViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val perfilRepository: PerfilRepository,
) : ViewModel() {

    private val estadoDaFoto = MutableStateFlow(EstadoDaFoto())

    val uiState: StateFlow<PerfilUiState> = combine(
        perfilRepository.observarPerfil(),
        perfilRepository.observarResumo(),
        estadoDaFoto,
    ) { perfil, resumo, foto ->
        criarEstado(perfil, resumo, foto)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PerfilUiState(),
    )

    private fun criarEstado(perfil: Perfil?, resumo: ResumoDoPerfil, foto: EstadoDaFoto): PerfilUiState {
        if (perfil == null) {
            return PerfilUiState(resumo = resumo, salvandoFoto = foto.salvando, mensagem = foto.mensagem)
        }
        val arquivo: File? = perfilRepository.arquivoDaFoto(perfil)
        return PerfilUiState(
            nome = perfil.nome,
            escola = perfil.escola,
            arquivoDaFoto = arquivo,
            temFoto = arquivo != null,
            resumo = resumo,
            salvandoFoto = foto.salvando,
            mensagem = foto.mensagem,
        )
    }

    fun salvarNomeEEscola(nome: String, escola: String) {
        viewModelScope.launch {
            perfilRepository.salvarNomeEEscola(nome, escola)
        }
    }

    /** Guarda o arquivo da câmera e devolve o endereço onde ela deve gravar. */
    fun prepararCamera(): Uri {
        val arquivo: ArquivoDaCamera = perfilRepository.prepararCamera()
        savedStateHandle[CHAVE_ARQUIVO_DA_CAMERA] = arquivo.nomeTemporario
        return arquivo.uri
    }

    /** [sucesso] é `false` quando a professora fecha a câmera sem tirar a foto. */
    fun fotoTirada(sucesso: Boolean) {
        val nomeTemporario: String = savedStateHandle[CHAVE_ARQUIVO_DA_CAMERA] ?: return
        savedStateHandle.remove<String>(CHAVE_ARQUIVO_DA_CAMERA)

        if (!sucesso) {
            viewModelScope.launch {
                perfilRepository.descartarCamera(nomeTemporario)
            }
            return
        }
        trocarFoto(aoTrocar = { perfilRepository.trocarFotoPelaCamera(nomeTemporario) })
    }

    /** O celular não tem app de câmera (raro, mas acontece em alguns tablets). */
    fun cameraIndisponivel() {
        val nomeTemporario: String? = savedStateHandle[CHAVE_ARQUIVO_DA_CAMERA]
        savedStateHandle.remove<String>(CHAVE_ARQUIVO_DA_CAMERA)
        if (nomeTemporario != null) {
            viewModelScope.launch {
                perfilRepository.descartarCamera(nomeTemporario)
            }
        }
        estadoDaFoto.value = estadoDaFoto.value.copy(mensagem = MensagemDoPerfil.SEM_CAMERA)
    }

    /** [escolhida] é `null` quando ela fecha a galeria sem escolher. */
    fun fotoEscolhida(escolhida: Uri?) {
        if (escolhida == null) {
            return
        }
        trocarFoto(aoTrocar = { perfilRepository.trocarFoto(escolhida) })
    }

    fun removerFoto() {
        viewModelScope.launch {
            perfilRepository.removerFoto()
        }
    }

    fun mensagemExibida() {
        estadoDaFoto.value = estadoDaFoto.value.copy(mensagem = null)
    }

    /** Reduzir a foto leva um instante: a tela mostra que está salvando e avisa se falhar. */
    private fun trocarFoto(aoTrocar: suspend () -> Unit) {
        estadoDaFoto.value = EstadoDaFoto(salvando = true)
        viewModelScope.launch {
            var mensagem: MensagemDoPerfil? = null
            try {
                aoTrocar()
            } catch (erro: IOException) {
                mensagem = MensagemDoPerfil.FOTO_NAO_LIDA
            } catch (erro: SecurityException) {
                mensagem = MensagemDoPerfil.FOTO_NAO_LIDA
            }
            estadoDaFoto.value = EstadoDaFoto(salvando = false, mensagem = mensagem)
        }
    }

    companion object {
        private const val CHAVE_ARQUIVO_DA_CAMERA = "perfil_arquivo_da_camera"
    }
}
