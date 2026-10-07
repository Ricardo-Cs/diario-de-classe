package br.com.ricardo.diariodeclasse.ui.metricas

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import br.com.ricardo.diariodeclasse.data.local.entity.Aluno
import br.com.ricardo.diariodeclasse.data.local.entity.Metrica
import br.com.ricardo.diariodeclasse.data.local.entity.NivelDaMetrica
import br.com.ricardo.diariodeclasse.data.local.entity.ResultadoDaSondagem
import br.com.ricardo.diariodeclasse.data.local.entity.ResultadoDatado
import br.com.ricardo.diariodeclasse.data.repository.AlunoRepository
import br.com.ricardo.diariodeclasse.data.repository.MarcacaoDeNivel
import br.com.ricardo.diariodeclasse.data.repository.MetricaRepository
import br.com.ricardo.diariodeclasse.data.repository.SondagemDoDia
import br.com.ricardo.diariodeclasse.ui.navigation.SondagemRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** Uma linha da tela: o aluno, o nível marcado agora e o da sondagem anterior. */
data class AlunoNaSondagem(
    val aluno: Aluno,
    /** `null` = não avaliado nesta sondagem. */
    val nivelId: String?,
    /** Nível na última sondagem antes desta data; `null` se nunca foi avaliado. */
    val nivelAnteriorId: String?,
) {
    /** Avaliado num nível diferente do anterior. "Não avaliado" não conta como mudança. */
    fun mudouDeNivel(): Boolean {
        return nivelId != null && nivelId != nivelAnteriorId
    }
}

sealed interface SondagemUiState {
    data object Carregando : SondagemUiState
    data object MetricaNaoEncontrada : SondagemUiState

    data class Carregado(
        val metrica: Metrica,
        /** Do mais inicial ao mais avançado. */
        val niveis: List<NivelDaMetrica>,
        val data: LocalDate,
        val alunos: List<AlunoNaSondagem>,
        /** As marcações como estavam ao abrir a tela, para saber se algo mudou. */
        val alunosAoAbrir: List<AlunoNaSondagem>,
        /** `true` quando já havia sondagem nesta data e a tela está editando. */
        val editando: Boolean,
        val salvando: Boolean = false,
        val salva: Boolean = false,
    ) : SondagemUiState {

        fun temAlteracoesNaoSalvas(): Boolean {
            return alunos != alunosAoAbrir
        }

        /** Algum aluno já tinha nível antes desta data (ou seja, não é a primeira sondagem). */
        fun temSondagemAnterior(): Boolean {
            for (linha in alunos) {
                if (linha.nivelAnteriorId != null) {
                    return true
                }
            }
            return false
        }

        fun quantidadeDeMudancas(): Int {
            var quantidade = 0
            for (linha in alunos) {
                if (linha.mudouDeNivel()) {
                    quantidade = quantidade + 1
                }
            }
            return quantidade
        }
    }
}

/**
 * Como a chamada, a sondagem é um formulário: os dados são lidos uma vez e as
 * marcações ficam na memória até a professora tocar em "Salvar".
 *
 * Numa sondagem nova, cada aluno já vem no nível da sondagem anterior: a
 * professora só toca em quem mudou. Na edição, vem o que foi gravado naquele dia.
 */
@HiltViewModel
class SondagemViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val metricaRepository: MetricaRepository,
    private val alunoRepository: AlunoRepository,
    clock: Clock,
) : ViewModel() {

    private val rota: SondagemRoute = savedStateHandle.toRoute<SondagemRoute>()
    private val data: LocalDate = LocalDate.parse(rota.data)

    private val estadoMutavel = MutableStateFlow<SondagemUiState>(SondagemUiState.Carregando)
    val uiState: StateFlow<SondagemUiState> = estadoMutavel

    init {
        viewModelScope.launch {
            carregar()
        }
    }

    private suspend fun carregar() {
        val metrica: Metrica? = metricaRepository.buscarMetrica(rota.metricaId)
        if (metrica == null) {
            estadoMutavel.value = SondagemUiState.MetricaNaoEncontrada
            return
        }

        val niveis: List<NivelDaMetrica> = metricaRepository.observarNiveis(metrica.id).first()
        val alunos: List<Aluno> = alunoRepository.observarAlunosDaTurma(metrica.turmaId).first()
        val sondagemSalva: SondagemDoDia? = metricaRepository.buscarSondagem(metrica.id, data)
        val todosOsResultados: List<ResultadoDatado> = metricaRepository.observarResultadosDaMetrica(metrica.id).first()
        val niveisAnteriores: Map<String, ResultadoDatado> = nivelAtualDeCadaAluno(todosOsResultados, data.minusDays(1))

        val linhas = mutableListOf<AlunoNaSondagem>()
        for (aluno in alunos) {
            linhas.add(criarLinha(aluno, sondagemSalva, niveisAnteriores))
        }

        estadoMutavel.value = SondagemUiState.Carregado(
            metrica = metrica,
            niveis = niveis,
            data = data,
            alunos = linhas,
            alunosAoAbrir = linhas.toList(),
            editando = sondagemSalva != null,
        )
    }

    private fun criarLinha(
        aluno: Aluno,
        sondagemSalva: SondagemDoDia?,
        niveisAnteriores: Map<String, ResultadoDatado>,
    ): AlunoNaSondagem {
        val resultadoAnterior: ResultadoDatado? = niveisAnteriores[aluno.id]
        var nivelAnteriorId: String? = null
        if (resultadoAnterior != null) {
            nivelAnteriorId = resultadoAnterior.nivelId
        }

        val nivelMarcado: String?
        if (sondagemSalva == null) {
            nivelMarcado = nivelAnteriorId
        } else {
            nivelMarcado = buscarNivelSalvo(sondagemSalva, aluno.id)
        }

        return AlunoNaSondagem(aluno = aluno, nivelId = nivelMarcado, nivelAnteriorId = nivelAnteriorId)
    }

    private fun buscarNivelSalvo(sondagemSalva: SondagemDoDia, alunoId: String): String? {
        for (resultado: ResultadoDaSondagem in sondagemSalva.resultados) {
            if (resultado.alunoId == alunoId) {
                return resultado.nivelId
            }
        }
        return null
    }

    /** `nivelId == null` marca o aluno como não avaliado (ex.: faltou no dia da sondagem). */
    fun escolherNivel(alunoId: String, nivelId: String?) {
        val estado: SondagemUiState = estadoMutavel.value
        if (estado !is SondagemUiState.Carregado) {
            return
        }
        val novasLinhas = mutableListOf<AlunoNaSondagem>()
        for (linha in estado.alunos) {
            if (linha.aluno.id == alunoId) {
                novasLinhas.add(linha.copy(nivelId = nivelId))
            } else {
                novasLinhas.add(linha)
            }
        }
        estadoMutavel.value = estado.copy(alunos = novasLinhas)
    }

    fun salvar() {
        val estado: SondagemUiState = estadoMutavel.value
        if (estado !is SondagemUiState.Carregado || estado.salvando) {
            return
        }
        estadoMutavel.value = estado.copy(salvando = true)

        val marcacoes = mutableListOf<MarcacaoDeNivel>()
        for (linha in estado.alunos) {
            marcacoes.add(MarcacaoDeNivel(alunoId = linha.aluno.id, nivelId = linha.nivelId))
        }

        viewModelScope.launch {
            metricaRepository.salvarSondagem(estado.metrica.id, estado.data, marcacoes)
            estadoMutavel.value = estado.copy(salvando = false, salva = true)
        }
    }
}
