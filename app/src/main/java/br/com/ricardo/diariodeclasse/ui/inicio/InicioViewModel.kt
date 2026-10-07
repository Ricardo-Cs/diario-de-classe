package br.com.ricardo.diariodeclasse.ui.inicio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.ricardo.diariodeclasse.data.local.entity.Lembrete
import br.com.ricardo.diariodeclasse.data.local.entity.Turma
import br.com.ricardo.diariodeclasse.data.repository.AlunoRepository
import br.com.ricardo.diariodeclasse.data.repository.ChamadaRepository
import br.com.ricardo.diariodeclasse.data.repository.LembreteRepository
import br.com.ricardo.diariodeclasse.data.repository.PendenciaRepository
import br.com.ricardo.diariodeclasse.data.repository.TurmaAtivaRepository
import br.com.ricardo.diariodeclasse.data.repository.TurmaRepository
import br.com.ricardo.diariodeclasse.ui.lembretes.lembretesDoInicio
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * O cabeçalho (saudação e data) aparece sempre; chamada e pendências dependem
 * das turmas. Os lembretes são da professora, não de uma turma: aparecem mesmo
 * sem turma cadastrada.
 */
data class InicioUiState(
    val saudacao: Saudacao,
    val hoje: LocalDate,
    val turmas: TurmasDoInicio,
    /** Em aberto, atrasados e dos próximos dias (ver `lembretesDoInicio`). */
    val lembretes: List<Lembrete>,
)

sealed interface TurmasDoInicio {
    data object Carregando : TurmasDoInicio
    data object NenhumaCadastrada : TurmasDoInicio
    data class Carregadas(
        val turmaAtiva: Turma,
        val todas: List<Turma>,
        val chamadaDeHoje: SituacaoDaChamada,
        val pendencias: ResumoDePendencias,
    ) : TurmasDoInicio
}

/** Turma ativa e a situação dela no dia (chamada e pendências), sempre calculadas juntas. */
private data class DadosDoDia(
    val turmaAtiva: Turma,
    val chamadaDeHoje: SituacaoDaChamada,
    val pendencias: ResumoDePendencias,
)

private data class TurmaNoDia(
    val turma: Turma?,
    val data: LocalDate,
)

@HiltViewModel
class InicioViewModel @Inject constructor(
    turmaRepository: TurmaRepository,
    private val turmaAtivaRepository: TurmaAtivaRepository,
    private val alunoRepository: AlunoRepository,
    private val chamadaRepository: ChamadaRepository,
    private val pendenciaRepository: PendenciaRepository,
    private val lembreteRepository: LembreteRepository,
    private val clock: Clock,
) : ViewModel() {

    /**
     * Data e hora em que o estado foi calculado. É atualizada pela tela sempre que
     * ela volta ao primeiro plano (ver [atualizarDataEHora]).
     */
    private val agora = MutableStateFlow(LocalDateTime.now(clock))

    /** `distinctUntilChanged` só deixa passar quando o dia de fato muda, não a cada minuto. */
    private val hoje: Flow<LocalDate> = agora
        .map { dataEHora -> dataEHora.toLocalDate() }
        .distinctUntilChanged()

    private val dadosDoDia: Flow<DadosDoDia?> = observarDadosDoDia()

    val uiState: StateFlow<InicioUiState> = combine(
        agora,
        dadosDoDia,
        turmaRepository.observarTurmas(),
        lembreteRepository.observarEmAberto(),
    ) { dataEHora, dados, todasAsTurmas, lembretesEmAberto ->
        criarEstado(dataEHora, dados, todasAsTurmas, lembretesEmAberto)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = criarEstadoCarregando(),
    )

    /**
     * Sempre que a turma ativa ou o dia mudam, troca as consultas de alunos e de
     * chamada pelas da nova turma/dia. `flatMapLatest` faz essa troca e cancela as
     * consultas antigas, como o `switchMap` do RxJS.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observarDadosDoDia(): Flow<DadosDoDia?> {
        return combine(turmaAtivaRepository.observarTurmaAtiva(), hoje) { turma, data ->
            TurmaNoDia(turma, data)
        }.flatMapLatest { turmaNoDia ->
            observarDadosDaTurma(turmaNoDia)
        }
    }

    private fun observarDadosDaTurma(turmaNoDia: TurmaNoDia): Flow<DadosDoDia?> {
        val turma: Turma? = turmaNoDia.turma
        if (turma == null) {
            return flowOf(null)
        }
        return combine(
            alunoRepository.observarAlunosDaTurma(turma.id),
            chamadaRepository.observarChamada(turma.id, turmaNoDia.data),
            pendenciaRepository.observarPendentesDaTurma(turma.id),
        ) { alunos, chamada, pendencias ->
            DadosDoDia(
                turmaAtiva = turma,
                chamadaDeHoje = calcularSituacaoDaChamada(alunos, chamada, clock.zone),
                pendencias = calcularResumoDePendencias(alunos, pendencias, turmaNoDia.data),
            )
        }
    }

    private fun criarEstado(
        dataEHora: LocalDateTime,
        dados: DadosDoDia?,
        todasAsTurmas: List<Turma>,
        lembretesEmAberto: List<Lembrete>,
    ): InicioUiState {
        val turmas: TurmasDoInicio
        if (dados == null) {
            turmas = TurmasDoInicio.NenhumaCadastrada
        } else {
            turmas = TurmasDoInicio.Carregadas(
                turmaAtiva = dados.turmaAtiva,
                todas = todasAsTurmas,
                chamadaDeHoje = dados.chamadaDeHoje,
                pendencias = dados.pendencias,
            )
        }

        val hoje: LocalDate = dataEHora.toLocalDate()
        return InicioUiState(
            saudacao = saudacaoParaHorario(dataEHora.toLocalTime()),
            hoje = hoje,
            turmas = turmas,
            lembretes = lembretesDoInicio(lembretesEmAberto, hoje),
        )
    }

    private fun criarEstadoCarregando(): InicioUiState {
        val dataEHora: LocalDateTime = agora.value
        return InicioUiState(
            saudacao = saudacaoParaHorario(dataEHora.toLocalTime()),
            hoje = dataEHora.toLocalDate(),
            turmas = TurmasDoInicio.Carregando,
            lembretes = emptyList(),
        )
    }

    fun marcarLembreteComoConcluido(lembreteId: String) {
        viewModelScope.launch {
            lembreteRepository.marcarComoConcluido(lembreteId)
        }
    }

    fun reabrirLembrete(lembreteId: String) {
        viewModelScope.launch {
            lembreteRepository.reabrir(lembreteId)
        }
    }

    /**
     * O app pode ficar aberto em segundo plano de manhã até a tarde (ou de um dia
     * para o outro); sem isto, a tela continuaria com a saudação, a data e a
     * chamada do dia anterior.
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
