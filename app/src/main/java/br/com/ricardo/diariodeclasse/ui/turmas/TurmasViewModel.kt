package br.com.ricardo.diariodeclasse.ui.turmas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.ricardo.diariodeclasse.data.local.entity.Turma
import br.com.ricardo.diariodeclasse.data.repository.TurmaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface TurmasUiState {
    data object Carregando : TurmasUiState
    data class Sucesso(val turmas: List<Turma>) : TurmasUiState
}

/**
 * `stateIn` transforma o Flow do banco num [StateFlow] (sempre tem valor atual, como um
 * BehaviorSubject do RxJS). `WhileSubscribed(5_000)` mantém a coleta por 5 s sem
 * observadores, para não reconsultar o banco a cada rotação de tela.
 */
@HiltViewModel
class TurmasViewModel @Inject constructor(
    turmaRepository: TurmaRepository,
) : ViewModel() {

    val uiState: StateFlow<TurmasUiState> = turmaRepository.observarTurmas()
        .map<List<Turma>, TurmasUiState> { TurmasUiState.Sucesso(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TurmasUiState.Carregando,
        )
}
