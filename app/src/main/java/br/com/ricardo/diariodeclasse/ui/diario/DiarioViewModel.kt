package br.com.ricardo.diariodeclasse.ui.diario

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.ricardo.diariodeclasse.data.local.entity.Aluno
import br.com.ricardo.diariodeclasse.data.local.entity.AlunoNaMeta
import br.com.ricardo.diariodeclasse.data.local.entity.Foto
import br.com.ricardo.diariodeclasse.data.local.entity.Meta
import br.com.ricardo.diariodeclasse.data.local.entity.Metrica
import br.com.ricardo.diariodeclasse.data.local.entity.NivelDaMetrica
import br.com.ricardo.diariodeclasse.data.local.entity.ResultadoDatado
import br.com.ricardo.diariodeclasse.data.local.entity.Turma
import br.com.ricardo.diariodeclasse.data.repository.AlunoRepository
import br.com.ricardo.diariodeclasse.data.repository.FotoRepository
import br.com.ricardo.diariodeclasse.data.repository.MetaRepository
import br.com.ricardo.diariodeclasse.data.repository.MetricaRepository
import br.com.ricardo.diariodeclasse.data.repository.TurmaAtivaRepository
import br.com.ricardo.diariodeclasse.data.repository.TurmaRepository
import br.com.ricardo.diariodeclasse.ui.fotos.FotoNaTela
import br.com.ricardo.diariodeclasse.ui.metas.ProgressoDaMeta
import br.com.ricardo.diariodeclasse.ui.metas.calcularProgressoDaMeta
import br.com.ricardo.diariodeclasse.ui.metricas.DistribuicaoDaMetrica
import br.com.ricardo.diariodeclasse.ui.metricas.calcularDistribuicao
import br.com.ricardo.diariodeclasse.ui.metricas.niveisDaMetrica
import br.com.ricardo.diariodeclasse.ui.metricas.nivelAtualDeCadaAluno
import br.com.ricardo.diariodeclasse.ui.metricas.resultadosDaMetrica
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

/** Uma métrica na lista do Diário, com o retrato atual da turma. */
data class ResumoDaMetrica(
    val metrica: Metrica,
    val distribuicao: DistribuicaoDaMetrica,
    /** `null` enquanto nenhuma sondagem foi feita. */
    val ultimaSondagem: LocalDate?,
)

data class ResumoDaMeta(
    val meta: Meta,
    val progresso: ProgressoDaMeta,
)

sealed interface DiarioUiState {
    data object Carregando : DiarioUiState
    data object NenhumaTurma : DiarioUiState

    data class Carregado(
        val turmaAtiva: Turma,
        val todasAsTurmas: List<Turma>,
        val hoje: LocalDate,
        /** Na ordem em que foram adicionadas. */
        val fotosDeHoje: List<FotoNaTela>,
        /** Se a turma tem alguma foto, de qualquer dia (mostra "Ver todas as fotos"). */
        val temFotos: Boolean,
        val metasEmAndamento: List<ResumoDaMeta>,
        val metasEncerradas: List<ResumoDaMeta>,
        val metricas: List<ResumoDaMetrica>,
    ) : DiarioUiState
}

/** Métricas, níveis e resultados da turma, que andam sempre juntos nos cálculos. */
private data class DadosDasMetricas(
    val metricas: List<Metrica>,
    val niveis: List<NivelDaMetrica>,
    val resultados: List<ResultadoDatado>,
)

/** O que a tela mostra da turma ativa, antes de juntar com a lista de turmas. */
private data class DadosDaTurma(
    val turma: Turma,
    val alunos: List<Aluno>,
    val metricas: DadosDasMetricas,
    val metas: List<Meta>,
    val alunosDasMetas: List<AlunoNaMeta>,
    val fotos: List<Foto>,
)

/**
 * Aba Diário: o registro e o acompanhamento da turma ativa (fotos do dia, metas
 * e métricas). Mesma turma ativa do Início; trocar aqui troca lá também.
 */
@HiltViewModel
class DiarioViewModel @Inject constructor(
    turmaRepository: TurmaRepository,
    private val turmaAtivaRepository: TurmaAtivaRepository,
    private val alunoRepository: AlunoRepository,
    private val metricaRepository: MetricaRepository,
    private val metaRepository: MetaRepository,
    private val fotoRepository: FotoRepository,
    private val clock: Clock,
) : ViewModel() {

    val uiState: StateFlow<DiarioUiState> = combine(
        observarDadosDaTurmaAtiva(),
        turmaRepository.observarTurmas(),
    ) { dados, todasAsTurmas ->
        criarEstado(dados, todasAsTurmas)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DiarioUiState.Carregando,
    )

    /** Troca todas as consultas quando a turma ativa muda (ver `InicioViewModel`). */
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

    /**
     * O `combine` com tipos só aceita até 5 Flows; por isso as três consultas
     * de métricas são combinadas antes, num Flow só.
     */
    private fun observarDadosDaTurma(turma: Turma): Flow<DadosDaTurma> {
        val metricas: Flow<DadosDasMetricas> = combine(
            metricaRepository.observarMetricasDaTurma(turma.id),
            metricaRepository.observarNiveisDaTurma(turma.id),
            metricaRepository.observarResultadosDaTurma(turma.id),
        ) { listaDeMetricas, niveis, resultados ->
            DadosDasMetricas(listaDeMetricas, niveis, resultados)
        }

        return combine(
            alunoRepository.observarAlunosDaTurma(turma.id),
            metricas,
            metaRepository.observarMetasDaTurma(turma.id),
            metaRepository.observarAlunosDasMetasDaTurma(turma.id),
            fotoRepository.observarDaTurma(turma.id),
        ) { alunos, dadosDasMetricas, metas, alunosDasMetas, fotos ->
            DadosDaTurma(turma, alunos, dadosDasMetricas, metas, alunosDasMetas, fotos)
        }
    }

    private fun criarEstado(dados: DadosDaTurma?, todasAsTurmas: List<Turma>): DiarioUiState {
        if (dados == null) {
            return DiarioUiState.NenhumaTurma
        }
        val hoje: LocalDate = LocalDate.now(clock)

        val emAndamento = mutableListOf<ResumoDaMeta>()
        val encerradas = mutableListOf<ResumoDaMeta>()
        for (meta in dados.metas) {
            val resumo: ResumoDaMeta = resumirMeta(meta, dados, hoje)
            if (meta.encerradaEm == null) {
                emAndamento.add(resumo)
            } else {
                encerradas.add(resumo)
            }
        }

        val metricas = mutableListOf<ResumoDaMetrica>()
        for (metrica in dados.metricas.metricas) {
            metricas.add(resumirMetrica(metrica, dados, hoje))
        }

        return DiarioUiState.Carregado(
            turmaAtiva = dados.turma,
            todasAsTurmas = todasAsTurmas,
            hoje = hoje,
            fotosDeHoje = fotosDoDia(dados.fotos, hoje),
            temFotos = dados.fotos.isNotEmpty(),
            metasEmAndamento = emAndamento,
            metasEncerradas = encerradas,
            metricas = metricas,
        )
    }

    private fun fotosDoDia(fotos: List<Foto>, dia: LocalDate): List<FotoNaTela> {
        val doDia = mutableListOf<FotoNaTela>()
        for (foto in fotos) {
            if (foto.data == dia) {
                doDia.add(FotoNaTela(foto, fotoRepository.arquivoDa(foto)))
            }
        }
        return doDia
    }

    private fun resumirMeta(meta: Meta, dados: DadosDaTurma, hoje: LocalDate): ResumoDaMeta {
        val alunosDestaMeta = mutableListOf<AlunoNaMeta>()
        for (linha in dados.alunosDasMetas) {
            if (linha.metaId == meta.id) {
                alunosDestaMeta.add(linha)
            }
        }
        // Meta marcada à mão não tem métrica: os níveis não são usados no cálculo.
        val metricaId: String? = meta.metricaId
        var niveis: List<NivelDaMetrica> = emptyList()
        if (metricaId != null) {
            niveis = niveisDaMetrica(dados.metricas.niveis, metricaId)
        }
        val progresso: ProgressoDaMeta = calcularProgressoDaMeta(
            meta = meta,
            niveis = niveis,
            alunosDaMeta = alunosDestaMeta,
            alunosDaTurma = dados.alunos,
            resultados = dados.metricas.resultados,
            hoje = hoje,
        )
        return ResumoDaMeta(meta, progresso)
    }

    private fun resumirMetrica(metrica: Metrica, dados: DadosDaTurma, hoje: LocalDate): ResumoDaMetrica {
        val resultados: List<ResultadoDatado> = resultadosDaMetrica(dados.metricas.resultados, metrica.id)
        val distribuicao: DistribuicaoDaMetrica = calcularDistribuicao(
            niveis = niveisDaMetrica(dados.metricas.niveis, metrica.id),
            alunos = dados.alunos,
            nivelAtual = nivelAtualDeCadaAluno(resultados, hoje),
        )
        return ResumoDaMetrica(metrica, distribuicao, dataDaUltimaSondagem(resultados, hoje))
    }

    private fun dataDaUltimaSondagem(resultados: List<ResultadoDatado>, hoje: LocalDate): LocalDate? {
        var ultima: LocalDate? = null
        for (resultado in resultados) {
            val antesDaUltima: Boolean = ultima != null && !resultado.data.isAfter(ultima)
            if (resultado.data.isAfter(hoje) || antesDaUltima) {
                continue
            }
            ultima = resultado.data
        }
        return ultima
    }

    fun selecionarTurma(turmaId: String) {
        viewModelScope.launch {
            turmaAtivaRepository.selecionar(turmaId)
        }
    }
}
