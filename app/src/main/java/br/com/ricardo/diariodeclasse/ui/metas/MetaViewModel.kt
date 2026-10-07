package br.com.ricardo.diariodeclasse.ui.metas

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import br.com.ricardo.diariodeclasse.data.local.entity.Aluno
import br.com.ricardo.diariodeclasse.data.local.entity.AlunoNaMeta
import br.com.ricardo.diariodeclasse.data.local.entity.Meta
import br.com.ricardo.diariodeclasse.data.local.entity.Metrica
import br.com.ricardo.diariodeclasse.data.local.entity.NivelDaMetrica
import br.com.ricardo.diariodeclasse.data.local.entity.ResultadoDatado
import br.com.ricardo.diariodeclasse.data.repository.AlunoRepository
import br.com.ricardo.diariodeclasse.data.repository.MetaRepository
import br.com.ricardo.diariodeclasse.data.repository.MetricaRepository
import br.com.ricardo.diariodeclasse.ui.navigation.MetaRoute
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

sealed interface MetaUiState {
    data object Carregando : MetaUiState
    data object MetaNaoEncontrada : MetaUiState

    data class Carregado(
        val meta: Meta,
        /** `null` nas metas marcadas à mão. */
        val metrica: Metrica?,
        val progresso: ProgressoDaMeta,
        val hoje: LocalDate,
    ) : MetaUiState
}

@HiltViewModel
class MetaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val metaRepository: MetaRepository,
    private val metricaRepository: MetricaRepository,
    private val alunoRepository: AlunoRepository,
    private val clock: Clock,
) : ViewModel() {

    private val metaId: String = savedStateHandle.toRoute<MetaRoute>().metaId

    /** A métrica e a turma só são conhecidas depois de ler a meta. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<MetaUiState> = metaRepository.observarMeta(metaId)
        .flatMapLatest { meta ->
            if (meta == null) {
                flowOf(MetaUiState.MetaNaoEncontrada)
            } else {
                observarMetaEncontrada(meta)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MetaUiState.Carregando,
        )

    private fun observarMetaEncontrada(meta: Meta): Flow<MetaUiState> {
        val metricaId: String? = meta.metricaId
        if (metricaId == null) {
            return observarMetaMarcadaAMao(meta)
        }
        return observarMetaPorMetrica(meta, metricaId)
    }

    /** Só precisa dos alunos: quem atingiu está gravado na própria meta. */
    private fun observarMetaMarcadaAMao(meta: Meta): Flow<MetaUiState> {
        return combine(
            metaRepository.observarAlunosDaMeta(meta.id),
            alunoRepository.observarAlunosDaTurma(meta.turmaId),
        ) { alunosDaMeta, alunosDaTurma ->
            val hoje: LocalDate = LocalDate.now(clock)
            MetaUiState.Carregado(
                meta = meta,
                metrica = null,
                progresso = calcularProgressoDaMeta(meta, emptyList(), alunosDaMeta, alunosDaTurma, emptyList(), hoje),
                hoje = hoje,
            )
        }
    }

    private fun observarMetaPorMetrica(meta: Meta, metricaId: String): Flow<MetaUiState> {
        return combine(
            metricaRepository.observarMetrica(metricaId),
            metricaRepository.observarNiveis(metricaId),
            metaRepository.observarAlunosDaMeta(meta.id),
            alunoRepository.observarAlunosDaTurma(meta.turmaId),
            metricaRepository.observarResultadosDaMetrica(metricaId),
        ) { metrica, niveis, alunosDaMeta, alunosDaTurma, resultados ->
            criarEstadoPorMetrica(meta, metrica, niveis, alunosDaMeta, alunosDaTurma, resultados)
        }
    }

    private fun criarEstadoPorMetrica(
        meta: Meta,
        metrica: Metrica?,
        niveis: List<NivelDaMetrica>,
        alunosDaMeta: List<AlunoNaMeta>,
        alunosDaTurma: List<Aluno>,
        resultados: List<ResultadoDatado>,
    ): MetaUiState {
        // A métrica foi excluída: a meta deixa de existir junto com ela.
        if (metrica == null) {
            return MetaUiState.MetaNaoEncontrada
        }
        val hoje: LocalDate = LocalDate.now(clock)
        return MetaUiState.Carregado(
            meta = meta,
            metrica = metrica,
            progresso = calcularProgressoDaMeta(meta, niveis, alunosDaMeta, alunosDaTurma, resultados, hoje),
            hoje = hoje,
        )
    }

    /** Metas marcadas à mão: marca o aluno como "atingiu" (com a data de hoje) ou desmarca. */
    fun alternarAtingiu(alunoId: String) {
        val estado: MetaUiState = uiState.value
        if (estado !is MetaUiState.Carregado || estado.meta.acompanhadaPorMetrica()) {
            return
        }

        var jaAtingiu = false
        for (item in estado.progresso.alunos) {
            if (item.aluno.id == alunoId && item.situacao == SituacaoNaMeta.ATINGIU) {
                jaAtingiu = true
            }
        }

        val atingiuEm: LocalDate?
        if (jaAtingiu) {
            atingiuEm = null
        } else {
            atingiuEm = LocalDate.now(clock)
        }
        viewModelScope.launch {
            metaRepository.marcarAtingiu(metaId, alunoId, atingiuEm)
        }
    }

    fun encerrar() {
        viewModelScope.launch {
            metaRepository.encerrar(metaId, LocalDate.now(clock))
        }
    }

    fun reabrir() {
        viewModelScope.launch {
            metaRepository.reabrir(metaId)
        }
    }
}
