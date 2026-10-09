package br.com.ricardo.diariodeclasse.ui.turmas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.ricardo.diariodeclasse.data.local.entity.Aluno
import br.com.ricardo.diariodeclasse.data.local.entity.Turma
import br.com.ricardo.diariodeclasse.data.repository.AlunoRepository
import br.com.ricardo.diariodeclasse.data.repository.TurmaAtivaRepository
import br.com.ricardo.diariodeclasse.data.repository.TurmaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface TurmaUiState {
    data object Carregando : TurmaUiState
    data object NenhumaTurma : TurmaUiState

    data class Carregado(
        val turma: Turma,
        /** Para o seletor de turma no topo da lista. */
        val todasAsTurmas: List<Turma>,
        val alunos: List<Aluno>,
    ) : TurmaUiState
}

/** A turma e os alunos dela, lidos juntos. */
private data class TurmaComAlunos(
    val turma: Turma,
    val alunos: List<Aluno>,
)

/**
 * Aba Turma: os alunos da turma ativa. Mesma turma ativa do Início, de "A fazer"
 * e do Acompanhamento; trocar aqui troca lá também.
 *
 * `combine` junta Flows e recalcula o estado sempre que qualquer um deles muda,
 * como o `combineLatest` do RxJS.
 */
@HiltViewModel
class TurmaViewModel @Inject constructor(
    turmaRepository: TurmaRepository,
    private val turmaAtivaRepository: TurmaAtivaRepository,
    private val alunoRepository: AlunoRepository,
) : ViewModel() {

    val uiState: StateFlow<TurmaUiState> = combine(
        observarTurmaAtivaComAlunos(),
        turmaRepository.observarTurmas(),
    ) { turmaComAlunos, todasAsTurmas ->
        criarEstado(turmaComAlunos, todasAsTurmas)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TurmaUiState.Carregando,
    )

    /** Troca a consulta de alunos quando a turma ativa muda (ver `PendenciasViewModel`). */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observarTurmaAtivaComAlunos(): Flow<TurmaComAlunos?> {
        return turmaAtivaRepository.observarTurmaAtiva().flatMapLatest { turma ->
            if (turma == null) {
                flowOf(null)
            } else {
                observarAlunos(turma)
            }
        }
    }

    private fun observarAlunos(turma: Turma): Flow<TurmaComAlunos> {
        return alunoRepository.observarAlunosDaTurma(turma.id).map { alunos ->
            TurmaComAlunos(turma, alunos)
        }
    }

    private fun criarEstado(turmaComAlunos: TurmaComAlunos?, todasAsTurmas: List<Turma>): TurmaUiState {
        if (turmaComAlunos == null) {
            return TurmaUiState.NenhumaTurma
        }
        return TurmaUiState.Carregado(
            turma = turmaComAlunos.turma,
            todasAsTurmas = todasAsTurmas,
            alunos = turmaComAlunos.alunos,
        )
    }

    fun selecionarTurma(turmaId: String) {
        viewModelScope.launch {
            turmaAtivaRepository.selecionar(turmaId)
        }
    }

    fun adicionarAluno(turmaId: String, nome: String) {
        val nomeSemEspacosNasPontas: String = nome.trim()
        if (nomeSemEspacosNasPontas.isEmpty()) {
            return
        }
        viewModelScope.launch {
            alunoRepository.criar(turmaId, nomeSemEspacosNasPontas)
        }
    }
}
