package br.com.ricardo.diariodeclasse.ui.pendencias

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.ricardo.diariodeclasse.data.local.entity.Aluno
import br.com.ricardo.diariodeclasse.data.local.entity.PendenciaComOrigem
import br.com.ricardo.diariodeclasse.data.local.entity.Turma
import br.com.ricardo.diariodeclasse.data.repository.AlunoRepository
import br.com.ricardo.diariodeclasse.data.repository.PendenciaRepository
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
    data object NenhumaTurma : PendenciasUiState

    data class Carregado(
        val turma: Turma,
        /** Para o seletor de turma no topo da lista. */
        val todasAsTurmas: List<Turma>,
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

/** As pendências e os alunos de uma turma, lidos juntos. */
private data class DadosDaTurma(
    val turma: Turma,
    val alunos: List<Aluno>,
    val pendencias: List<PendenciaComOrigem>,
)

/**
 * Pendências da turma ativa, na aba "A fazer". Mesma turma ativa do Início e
 * do Diário; trocar aqui troca lá também.
 */
@HiltViewModel
class PendenciasViewModel @Inject constructor(
    turmaRepository: TurmaRepository,
    private val turmaAtivaRepository: TurmaAtivaRepository,
    private val alunoRepository: AlunoRepository,
    private val pendenciaRepository: PendenciaRepository,
    private val clock: Clock,
) : ViewModel() {

    val uiState: StateFlow<PendenciasUiState> = combine(
        observarDadosDaTurmaAtiva(),
        turmaRepository.observarTurmas(),
    ) { dados, todasAsTurmas ->
        criarEstado(dados, todasAsTurmas)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PendenciasUiState.Carregando,
    )

    /**
     * `flatMapLatest` troca as consultas quando a turma ativa muda: a consulta da
     * turma anterior é cancelada e a da nova começa (como o `switchMap` do RxJS).
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observarDadosDaTurmaAtiva(): Flow<DadosDaTurma?> {
        return turmaAtivaRepository.observarTurmaAtiva().flatMapLatest { turma ->
            if (turma == null) {
                flowOf(null)
            } else {
                observarDadosDaTurma(turma)
            }
        }
    }

    private fun observarDadosDaTurma(turma: Turma): Flow<DadosDaTurma> {
        return combine(
            alunoRepository.observarAlunosDaTurma(turma.id),
            pendenciaRepository.observarPendentesDaTurma(turma.id),
        ) { alunos, pendencias ->
            DadosDaTurma(turma, alunos, pendencias)
        }
    }

    private fun criarEstado(dados: DadosDaTurma?, todasAsTurmas: List<Turma>): PendenciasUiState {
        if (dados == null) {
            return PendenciasUiState.NenhumaTurma
        }
        // Lido a cada atualização, e não uma vez só: a aba pode ficar aberta de um dia para o outro.
        val hoje: LocalDate = LocalDate.now(clock)
        return PendenciasUiState.Carregado(
            turma = dados.turma,
            todasAsTurmas = todasAsTurmas,
            hoje = hoje,
            grupos = agruparPorAluno(dados.alunos, dados.pendencias),
            alunos = dados.alunos,
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

    fun selecionarTurma(turmaId: String) {
        viewModelScope.launch {
            turmaAtivaRepository.selecionar(turmaId)
        }
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
