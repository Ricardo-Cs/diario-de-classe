package br.com.ricardo.diariodeclasse.ui.fotos

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import br.com.ricardo.diariodeclasse.data.local.entity.Foto
import br.com.ricardo.diariodeclasse.data.repository.FotoRepository
import br.com.ricardo.diariodeclasse.ui.navigation.FotoRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

sealed interface FotoUiState {
    data object Carregando : FotoUiState

    /** A turma não tem mais fotos (ex.: a última acabou de ser excluída). */
    data object SemFotos : FotoUiState

    data class Carregado(
        val hoje: LocalDate,
        /** Todas as fotos da turma, na ordem da linha do tempo, para passar de uma a outra. */
        val fotos: List<FotoNaTela>,
        /** Posição, em [fotos], da foto que a professora tocou. */
        val posicaoInicial: Int,
    ) : FotoUiState
}

/** A foto em tela cheia, deslizando para ver as outras da turma. */
@HiltViewModel
class FotoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val fotoRepository: FotoRepository,
    private val clock: Clock,
) : ViewModel() {

    private val rota: FotoRoute = savedStateHandle.toRoute<FotoRoute>()

    val uiState: StateFlow<FotoUiState> = fotoRepository.observarDaTurma(rota.turmaId)
        .map { fotos -> criarEstado(fotos) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FotoUiState.Carregando,
        )

    private fun criarEstado(fotos: List<Foto>): FotoUiState {
        if (fotos.isEmpty()) {
            return FotoUiState.SemFotos
        }
        val fotosNaTela = mutableListOf<FotoNaTela>()
        var posicaoInicial = 0
        for (foto in fotos) {
            if (foto.id == rota.fotoId) {
                posicaoInicial = fotosNaTela.size
            }
            fotosNaTela.add(FotoNaTela(foto, fotoRepository.arquivoDa(foto)))
        }
        return FotoUiState.Carregado(
            hoje = LocalDate.now(clock),
            fotos = fotosNaTela,
            posicaoInicial = posicaoInicial,
        )
    }

    fun editarLegenda(fotoId: String, legenda: String) {
        viewModelScope.launch {
            fotoRepository.editarLegenda(fotoId, legenda)
        }
    }

    fun excluir(fotoId: String) {
        viewModelScope.launch {
            fotoRepository.excluir(fotoId)
        }
    }
}
