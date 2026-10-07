package br.com.ricardo.diariodeclasse.ui.lembretes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.ricardo.diariodeclasse.data.local.entity.Lembrete
import br.com.ricardo.diariodeclasse.data.repository.LembreteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

sealed interface LembretesUiState {
    data object Carregando : LembretesUiState

    data class Carregado(
        val hoje: LocalDate,
        /** Por data: atrasados e mais próximos primeiro. */
        val emAberto: List<Lembrete>,
        /** Concluídos há pouco, mais recentes primeiro. */
        val concluidos: List<Lembrete>,
    ) : LembretesUiState
}

@HiltViewModel
class LembretesViewModel @Inject constructor(
    private val lembreteRepository: LembreteRepository,
    private val clock: Clock,
) : ViewModel() {

    val uiState: StateFlow<LembretesUiState> = combine(
        lembreteRepository.observarEmAberto(),
        lembreteRepository.observarConcluidosRecentes(),
    ) { emAberto, concluidos ->
        LembretesUiState.Carregado(hoje = LocalDate.now(clock), emAberto = emAberto, concluidos = concluidos)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LembretesUiState.Carregando,
    )

    fun criar(descricao: String, data: LocalDate) {
        val descricaoLimpa: String = descricao.trim()
        if (descricaoLimpa.isEmpty()) {
            return
        }
        viewModelScope.launch {
            lembreteRepository.criar(descricaoLimpa, data)
        }
    }

    fun editar(lembreteId: String, descricao: String, data: LocalDate) {
        val descricaoLimpa: String = descricao.trim()
        if (descricaoLimpa.isEmpty()) {
            return
        }
        viewModelScope.launch {
            lembreteRepository.editar(lembreteId, descricaoLimpa, data)
        }
    }

    fun marcarComoConcluido(lembreteId: String) {
        viewModelScope.launch {
            lembreteRepository.marcarComoConcluido(lembreteId)
        }
    }

    fun reabrir(lembreteId: String) {
        viewModelScope.launch {
            lembreteRepository.reabrir(lembreteId)
        }
    }

    fun excluir(lembreteId: String) {
        viewModelScope.launch {
            lembreteRepository.excluir(lembreteId)
        }
    }

    fun restaurar(lembreteId: String) {
        viewModelScope.launch {
            lembreteRepository.restaurar(lembreteId)
        }
    }
}
