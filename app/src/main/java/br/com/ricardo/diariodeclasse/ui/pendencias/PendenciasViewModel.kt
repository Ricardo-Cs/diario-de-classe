package br.com.ricardo.diariodeclasse.ui.pendencias

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import br.com.ricardo.diariodeclasse.data.local.entity.Aluno
import br.com.ricardo.diariodeclasse.data.local.entity.PendenciaComOrigem
import br.com.ricardo.diariodeclasse.data.local.entity.Turma
import br.com.ricardo.diariodeclasse.data.repository.AlunoRepository
import br.com.ricardo.diariodeclasse.data.repository.PendenciaRepository
import br.com.ricardo.diariodeclasse.data.repository.TurmaRepository
import br.com.ricardo.diariodeclasse.ui.navigation.PendenciasRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** As pendências de um aluno, para a lista agrupada. */
data class GrupoDePendencias(
    val aluno: Aluno,
    val pendencias: List<PendenciaComOrigem>,
)

sealed interface PendenciasUiState {
    data object Carregando : PendenciasUiState
    data object TurmaNaoEncontrada : PendenciasUiState

    data class Carregado(
        val turma: Turma,
        val hoje: LocalDate,
        val grupos: List<GrupoDePendencias>,
        /** Todos os alunos da turma, para escolher ao criar uma pendência. */
        val alunos: List<Aluno>,
    ) : PendenciasUiState {

        fun quantidadeTotal(): Int {
            var quantidade = 0
            for (grupo in grupos) {
                quantidade = quantidade + grupo.pendencias.size
            }
            return quantidade
        }

        fun quantidadeParaHoje(): Int {
            var quantidade = 0
            for (grupo in grupos) {
                for (item in grupo.pendencias) {
                    if (item.pendencia.estaPendenteEm(hoje)) {
                        quantidade = quantidade + 1
                    }
                }
            }
            return quantidade
        }
    }
}

@HiltViewModel
class PendenciasViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    turmaRepository: TurmaRepository,
    alunoRepository: AlunoRepository,
    private val pendenciaRepository: PendenciaRepository,
    clock: Clock,
) : ViewModel() {

    private val turmaId: String = savedStateHandle.toRoute<PendenciasRoute>().turmaId
    private val hoje: LocalDate = LocalDate.now(clock)

    val uiState: StateFlow<PendenciasUiState> = combine(
        turmaRepository.observarTurma(turmaId),
        alunoRepository.observarAlunosDaTurma(turmaId),
        pendenciaRepository.observarPendentesDaTurma(turmaId),
    ) { turma, alunos, pendencias ->
        criarEstado(turma, alunos, pendencias)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PendenciasUiState.Carregando,
    )

    private fun criarEstado(turma: Turma?, alunos: List<Aluno>, pendencias: List<PendenciaComOrigem>): PendenciasUiState {
        if (turma == null) {
            return PendenciasUiState.TurmaNaoEncontrada
        }
        return PendenciasUiState.Carregado(
            turma = turma,
            hoje = hoje,
            grupos = agruparPorAluno(alunos, pendencias),
            alunos = alunos,
        )
    }

    /**
     * Segue a ordem alfabética dos alunos e deixa de fora quem não tem pendências.
     * Dentro de cada grupo, mantém a ordem do banco (lembrete mais antigo primeiro).
     */
    private fun agruparPorAluno(alunos: List<Aluno>, pendencias: List<PendenciaComOrigem>): List<GrupoDePendencias> {
        val grupos = mutableListOf<GrupoDePendencias>()
        for (aluno in alunos) {
            val pendenciasDoAluno = mutableListOf<PendenciaComOrigem>()
            for (item in pendencias) {
                if (item.pendencia.alunoId == aluno.id) {
                    pendenciasDoAluno.add(item)
                }
            }
            if (pendenciasDoAluno.isNotEmpty()) {
                grupos.add(GrupoDePendencias(aluno, pendenciasDoAluno))
            }
        }
        return grupos
    }

    fun criarPendencia(alunoId: String, descricao: String, dataLembrete: LocalDate) {
        val descricaoLimpa: String = descricao.trim()
        if (descricaoLimpa.isEmpty()) {
            return
        }
        viewModelScope.launch {
            pendenciaRepository.criar(alunoId, descricaoLimpa, dataLembrete)
        }
    }

    fun editarPendencia(pendenciaId: String, descricao: String, dataLembrete: LocalDate) {
        val descricaoLimpa: String = descricao.trim()
        if (descricaoLimpa.isEmpty()) {
            return
        }
        viewModelScope.launch {
            pendenciaRepository.editar(pendenciaId, descricaoLimpa, dataLembrete)
        }
    }

    fun marcarComoEntregue(pendenciaId: String) {
        viewModelScope.launch {
            pendenciaRepository.marcarComoEntregue(pendenciaId)
        }
    }

    fun desfazerEntrega(pendenciaId: String) {
        viewModelScope.launch {
            pendenciaRepository.desfazerEntrega(pendenciaId)
        }
    }

    fun excluirPendencia(pendenciaId: String) {
        viewModelScope.launch {
            pendenciaRepository.excluir(pendenciaId)
        }
    }

    fun restaurarPendencia(pendenciaId: String) {
        viewModelScope.launch {
            pendenciaRepository.restaurar(pendenciaId)
        }
    }
}
