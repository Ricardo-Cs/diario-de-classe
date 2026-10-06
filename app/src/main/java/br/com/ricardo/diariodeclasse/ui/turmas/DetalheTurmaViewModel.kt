package br.com.ricardo.diariodeclasse.ui.turmas

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import br.com.ricardo.diariodeclasse.data.local.entity.Aluno
import br.com.ricardo.diariodeclasse.data.local.entity.Turma
import br.com.ricardo.diariodeclasse.data.repository.AlunoRepository
import br.com.ricardo.diariodeclasse.data.repository.TurmaRepository
import br.com.ricardo.diariodeclasse.ui.navigation.DetalheTurmaRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface DetalheTurmaUiState {
    data object Carregando : DetalheTurmaUiState
    data object TurmaNaoEncontrada : DetalheTurmaUiState
    data class Carregado(val turma: Turma, val alunos: List<Aluno>) : DetalheTurmaUiState
}

/**
 * `combine` junta dois Flows (a turma e seus alunos) e recalcula o estado
 * sempre que qualquer um deles muda, como o `combineLatest` do RxJS.
 */
@HiltViewModel
class DetalheTurmaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val turmaRepository: TurmaRepository,
    private val alunoRepository: AlunoRepository,
) : ViewModel() {

    private val turmaId: String = savedStateHandle.toRoute<DetalheTurmaRoute>().turmaId

    val uiState: StateFlow<DetalheTurmaUiState> = combine(
        turmaRepository.observarTurma(turmaId),
        alunoRepository.observarAlunosDaTurma(turmaId),
    ) { turma, alunos ->
        criarEstado(turma, alunos)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DetalheTurmaUiState.Carregando,
    )

    private fun criarEstado(turma: Turma?, alunos: List<Aluno>): DetalheTurmaUiState {
        if (turma == null) {
            return DetalheTurmaUiState.TurmaNaoEncontrada
        }
        return DetalheTurmaUiState.Carregado(turma, alunos)
    }

    fun adicionarAluno(nome: String) {
        val nomeSemEspacosNasPontas: String = nome.trim()
        if (nomeSemEspacosNasPontas.isEmpty()) {
            return
        }
        viewModelScope.launch {
            alunoRepository.criar(turmaId, nomeSemEspacosNasPontas)
        }
    }
}
