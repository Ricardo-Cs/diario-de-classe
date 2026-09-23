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

sealed interface ListaTurmasUiState {
    data object Carregando : ListaTurmasUiState
    data class Carregado(val turmas: List<Turma>) : ListaTurmasUiState
}

/**
 * `stateIn` transforma o Flow do banco num [StateFlow] (sempre tem valor atual, como um
 * BehaviorSubject do RxJS). `WhileSubscribed(5_000)` mantém a coleta por 5 s sem
 * observadores, para não reconsultar o banco a cada rotação de tela.
 */
@HiltViewModel
class ListaTurmasViewModel @Inject constructor(
    turmaRepository: TurmaRepository,
) : ViewModel() {

    val uiState: StateFlow<ListaTurmasUiState> = turmaRepository.observarTurmas()
        .map { turmas -> ListaTurmasUiState.Carregado(turmas) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ListaTurmasUiState.Carregando,
        )
}
