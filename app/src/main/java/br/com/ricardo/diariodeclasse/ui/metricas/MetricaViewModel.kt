package br.com.ricardo.diariodeclasse.ui.metricas

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import br.com.ricardo.diariodeclasse.data.local.entity.Aluno
import br.com.ricardo.diariodeclasse.data.local.entity.Metrica
import br.com.ricardo.diariodeclasse.data.local.entity.NivelDaMetrica
import br.com.ricardo.diariodeclasse.data.local.entity.ResultadoDatado
import br.com.ricardo.diariodeclasse.data.local.entity.SondagemResumida
import br.com.ricardo.diariodeclasse.data.repository.AlunoRepository
import br.com.ricardo.diariodeclasse.data.repository.MetricaRepository
import br.com.ricardo.diariodeclasse.ui.navigation.MetricaRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

sealed interface MetricaUiState {
    data object Carregando : MetricaUiState
    data object MetricaNaoEncontrada : MetricaUiState

    data class Carregado(
        val metrica: Metrica,
        val hoje: LocalDate,
        val distribuicao: DistribuicaoDaMetrica,
        /** Da mais recente para a mais antiga. */
        val sondagens: List<SondagemResumida>,
        val temAlunos: Boolean,
    ) : MetricaUiState {

        fun temSondagemHoje(): Boolean {
            for (sondagem in sondagens) {
                if (sondagem.data == hoje) {
                    return true
                }
            }
            return false
        }

        fun totalDeAlunos(): Int {
            var total: Int = distribuicao.semAvaliacao
            for (faixa in distribuicao.faixas) {
                total = total + faixa.quantidade
            }
            return total
        }
    }
}

@HiltViewModel
class MetricaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val metricaRepository: MetricaRepository,
    private val alunoRepository: AlunoRepository,
    private val clock: Clock,
) : ViewModel() {

    private val metricaId: String = savedStateHandle.toRoute<MetricaRoute>().metricaId

    /** Os alunos dependem da turma, que só se conhece depois de ler a métrica. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<MetricaUiState> = metricaRepository.observarMetrica(metricaId)
        .flatMapLatest { metrica ->
            if (metrica == null) {
                flowOf(MetricaUiState.MetricaNaoEncontrada)
            } else {
                observarMetricaEncontrada(metrica)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MetricaUiState.Carregando,
        )

    private fun observarMetricaEncontrada(metrica: Metrica): Flow<MetricaUiState> {
        return combine(
            metricaRepository.observarNiveis(metrica.id),
            alunoRepository.observarAlunosDaTurma(metrica.turmaId),
            metricaRepository.observarResultadosDaMetrica(metrica.id),
            metricaRepository.observarSondagens(metrica.id),
        ) { niveis, alunos, resultados, sondagens ->
            criarEstado(metrica, niveis, alunos, resultados, sondagens)
        }
    }

    private fun criarEstado(
        metrica: Metrica,
        niveis: List<NivelDaMetrica>,
        alunos: List<Aluno>,
        resultados: List<ResultadoDatado>,
        sondagens: List<SondagemResumida>,
    ): MetricaUiState {
        val hoje: LocalDate = LocalDate.now(clock)
        return MetricaUiState.Carregado(
            metrica = metrica,
            hoje = hoje,
            distribuicao = calcularDistribuicao(niveis, alunos, nivelAtualDeCadaAluno(resultados, hoje)),
            sondagens = sondagens,
            temAlunos = alunos.isNotEmpty(),
        )
    }
}
