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
        val metrica: Metrica,
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
        return combine(
            metricaRepository.observarMetrica(meta.metricaId),
            metricaRepository.observarNiveis(meta.metricaId),
            metaRepository.observarAlunosDaMeta(meta.id),
            alunoRepository.observarAlunosDaTurma(meta.turmaId),
            metricaRepository.observarResultadosDaMetrica(meta.metricaId),
        ) { metrica, niveis, alunosDaMeta, alunosDaTurma, resultados ->
            criarEstado(meta, metrica, niveis, alunosDaMeta, alunosDaTurma, resultados)
        }
    }

    private fun criarEstado(
        meta: Meta,
        metrica: Metrica?,
        niveis: List<NivelDaMetrica>,
        alunosDaMeta: List<AlunoNaMeta>,
        alunosDaTurma: List<Aluno>,
        resultados: List<ResultadoDatado>,
    ): MetaUiState {
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
