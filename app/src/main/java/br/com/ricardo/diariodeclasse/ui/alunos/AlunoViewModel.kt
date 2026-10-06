package br.com.ricardo.diariodeclasse.ui.alunos

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import br.com.ricardo.diariodeclasse.data.local.entity.Aluno
import br.com.ricardo.diariodeclasse.data.local.entity.Anotacao
import br.com.ricardo.diariodeclasse.data.repository.AlunoRepository
import br.com.ricardo.diariodeclasse.data.repository.AnotacaoRepository
import br.com.ricardo.diariodeclasse.ui.navigation.AlunoRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

sealed interface AlunoUiState {
    data object Carregando : AlunoUiState
    data object AlunoNaoEncontrado : AlunoUiState

    data class Carregado(
        val aluno: Aluno,
        val hoje: LocalDate,
        /** Mais recentes primeiro. */
        val anotacoes: List<Anotacao>,
    ) : AlunoUiState
}

/** Tela do aluno: por enquanto, as anotações sobre ele e a edição do cadastro. */
@HiltViewModel
class AlunoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val alunoRepository: AlunoRepository,
    private val anotacaoRepository: AnotacaoRepository,
    private val clock: Clock,
) : ViewModel() {

    private val alunoId: String = savedStateHandle.toRoute<AlunoRoute>().alunoId

    val uiState: StateFlow<AlunoUiState> = combine(
        alunoRepository.observarAluno(alunoId),
        anotacaoRepository.observarDoAluno(alunoId),
    ) { aluno, anotacoes ->
        criarEstado(aluno, anotacoes)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AlunoUiState.Carregando,
    )

    private fun criarEstado(aluno: Aluno?, anotacoes: List<Anotacao>): AlunoUiState {
        if (aluno == null) {
            return AlunoUiState.AlunoNaoEncontrado
        }
        return AlunoUiState.Carregado(
            aluno = aluno,
            hoje = LocalDate.now(clock),
            anotacoes = anotacoes,
        )
    }

    /** A anotação nova recebe a data de hoje. */
    fun criarAnotacao(texto: String) {
        val textoLimpo: String = texto.trim()
        if (textoLimpo.isEmpty()) {
            return
        }
        viewModelScope.launch {
            anotacaoRepository.criar(alunoId, textoLimpo, LocalDate.now(clock))
        }
    }

    fun editarAnotacao(anotacaoId: String, texto: String) {
        val textoLimpo: String = texto.trim()
        if (textoLimpo.isEmpty()) {
            return
        }
        viewModelScope.launch {
            anotacaoRepository.editarTexto(anotacaoId, textoLimpo)
        }
    }

    fun excluirAnotacao(anotacaoId: String) {
        viewModelScope.launch {
            anotacaoRepository.excluir(anotacaoId)
        }
    }

    fun restaurarAnotacao(anotacaoId: String) {
        viewModelScope.launch {
            anotacaoRepository.restaurar(anotacaoId)
        }
    }

    fun renomearAluno(aluno: Aluno, novoNome: String) {
        val nomeSemEspacosNasPontas: String = novoNome.trim()
        if (nomeSemEspacosNasPontas.isEmpty()) {
            return
        }
        viewModelScope.launch {
            alunoRepository.renomear(aluno, nomeSemEspacosNasPontas)
        }
    }

    /** Depois de excluir, o Flow do aluno emite `null` e a tela volta sozinha. */
    fun excluirAluno() {
        viewModelScope.launch {
            alunoRepository.excluir(alunoId)
        }
    }
}
