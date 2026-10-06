package br.com.ricardo.diariodeclasse.ui.inicio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.ricardo.diariodeclasse.data.local.entity.Turma
import br.com.ricardo.diariodeclasse.data.repository.TurmaAtivaRepository
import br.com.ricardo.diariodeclasse.data.repository.TurmaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

/** O cabeçalho (saudação e data) aparece sempre; o resto depende das turmas. */
data class InicioUiState(
    val saudacao: Saudacao,
    val hoje: LocalDate,
    val turmas: TurmasDoInicio,
)

sealed interface TurmasDoInicio {
    data object Carregando : TurmasDoInicio
    data object NenhumaCadastrada : TurmasDoInicio
    data class Carregadas(val turmaAtiva: Turma, val todas: List<Turma>) : TurmasDoInicio
}

@HiltViewModel
class InicioViewModel @Inject constructor(
    turmaRepository: TurmaRepository,
    private val turmaAtivaRepository: TurmaAtivaRepository,
    private val clock: Clock,
) : ViewModel() {

    /**
     * Data e hora em que o estado foi calculado. É atualizada pela tela sempre que
     * ela volta ao primeiro plano (ver [atualizarDataEHora]).
     */
    private val agora = MutableStateFlow(LocalDateTime.now(clock))

    val uiState: StateFlow<InicioUiState> = combine(
        agora,
        turmaAtivaRepository.observarTurmaAtiva(),
        turmaRepository.observarTurmas(),
    ) { dataEHora, turmaAtiva, todasAsTurmas ->
        criarEstado(dataEHora, turmaAtiva, todasAsTurmas)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = criarEstadoCarregando(),
    )

    private fun criarEstado(
        dataEHora: LocalDateTime,
        turmaAtiva: Turma?,
        todasAsTurmas: List<Turma>,
    ): InicioUiState {
        val turmas: TurmasDoInicio
        if (turmaAtiva == null) {
            turmas = TurmasDoInicio.NenhumaCadastrada
        } else {
            turmas = TurmasDoInicio.Carregadas(turmaAtiva, todasAsTurmas)
        }

        return InicioUiState(
            saudacao = saudacaoParaHorario(dataEHora.toLocalTime()),
            hoje = dataEHora.toLocalDate(),
            turmas = turmas,
        )
    }

    private fun criarEstadoCarregando(): InicioUiState {
        val dataEHora: LocalDateTime = agora.value
        return InicioUiState(
            saudacao = saudacaoParaHorario(dataEHora.toLocalTime()),
            hoje = dataEHora.toLocalDate(),
            turmas = TurmasDoInicio.Carregando,
        )
    }

    /**
     * O app pode ficar aberto em segundo plano de manhã até a tarde (ou de um dia
     * para o outro); sem isto, a tela continuaria com a saudação e a data antigas.
     */
    fun atualizarDataEHora() {
        agora.value = LocalDateTime.now(clock)
    }

    fun selecionarTurma(turmaId: String) {
        viewModelScope.launch {
            turmaAtivaRepository.selecionar(turmaId)
        }
    }
}
