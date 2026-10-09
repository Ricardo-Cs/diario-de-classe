package br.com.ricardo.diariodeclasse.ui.fotos

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import br.com.ricardo.diariodeclasse.data.local.entity.Foto
import br.com.ricardo.diariodeclasse.data.local.entity.Turma
import br.com.ricardo.diariodeclasse.data.repository.FotoRepository
import br.com.ricardo.diariodeclasse.data.repository.TurmaRepository
import br.com.ricardo.diariodeclasse.ui.navigation.FotosRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

sealed interface FotosUiState {
    data object Carregando : FotosUiState
    data object TurmaNaoEncontrada : FotosUiState

    data class Carregado(
        val turma: Turma,
        val hoje: LocalDate,
        /** Dias mais recentes primeiro; só os dias que têm fotos. */
        val dias: List<DiaDeFotos>,
    ) : FotosUiState
}

/** Linha do tempo das fotos de uma turma, agrupadas por dia. */
@HiltViewModel
class FotosViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    turmaRepository: TurmaRepository,
    private val fotoRepository: FotoRepository,
    private val clock: Clock,
) : ViewModel() {

    val turmaId: String = savedStateHandle.toRoute<FotosRoute>().turmaId

    val uiState: StateFlow<FotosUiState> = combine(
        turmaRepository.observarTurma(turmaId),
        fotoRepository.observarDaTurma(turmaId),
    ) { turma, fotos ->
        criarEstado(turma, fotos)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FotosUiState.Carregando,
    )

    private fun criarEstado(turma: Turma?, fotos: List<Foto>): FotosUiState {
        if (turma == null) {
            return FotosUiState.TurmaNaoEncontrada
        }
        val fotosNaTela = mutableListOf<FotoNaTela>()
        for (foto in fotos) {
            fotosNaTela.add(FotoNaTela(foto, fotoRepository.arquivoDa(foto)))
        }
        return FotosUiState.Carregado(
            turma = turma,
            hoje = LocalDate.now(clock),
            dias = agruparPorDia(fotosNaTela),
        )
    }
}
