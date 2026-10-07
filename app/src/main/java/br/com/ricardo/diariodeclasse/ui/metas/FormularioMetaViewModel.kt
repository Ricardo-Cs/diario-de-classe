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
import br.com.ricardo.diariodeclasse.data.repository.AlunoEscolhidoParaMeta
import br.com.ricardo.diariodeclasse.data.repository.AlunoRepository
import br.com.ricardo.diariodeclasse.data.repository.MetaRepository
import br.com.ricardo.diariodeclasse.data.repository.MetricaRepository
import br.com.ricardo.diariodeclasse.ui.metricas.buscarNivel
import br.com.ricardo.diariodeclasse.ui.metricas.niveisDaMetrica
import br.com.ricardo.diariodeclasse.ui.metricas.nivelAtualDeCadaAluno
import br.com.ricardo.diariodeclasse.ui.metricas.resultadosDaMetrica
import br.com.ricardo.diariodeclasse.ui.navigation.FormularioMetaRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** Um aluno da turma na lista de escolha, com o nível em que está hoje na métrica da meta. */
data class AlunoParaEscolher(
    val aluno: Aluno,
    val nivelAtual: NivelDaMetrica?,
    val escolhido: Boolean,
)

/** Atalho "Silábico com valor sonoro (11)": escolhe de uma vez quem está naquele nível. */
data class AtalhoDeNivel(
    val nivel: NivelDaMetrica,
    val quantidade: Int,
)

data class FormularioMetaUiState(
    val carregando: Boolean,
    val editando: Boolean,
    val hoje: LocalDate,
    val descricao: String = "",
    val metricas: List<Metrica> = emptyList(),
    val metricaId: String? = null,
    /** Níveis da métrica escolhida, do mais inicial ao mais avançado. */
    val niveis: List<NivelDaMetrica> = emptyList(),
    val nivelAlvoId: String? = null,
    val prazo: LocalDate? = null,
    val alunos: List<AlunoParaEscolher> = emptyList(),
    val salvando: Boolean = false,
    val salvo: Boolean = false,
    val excluida: Boolean = false,
) {
    fun quantidadeEscolhida(): Int {
        var quantidade = 0
        for (item in alunos) {
            if (item.escolhido) {
                quantidade = quantidade + 1
            }
        }
        return quantidade
    }

    /** Um atalho por nível que tem alguém hoje. */
    fun atalhos(): List<AtalhoDeNivel> {
        val atalhos = mutableListOf<AtalhoDeNivel>()
        for (nivel in niveis) {
            var quantidade = 0
            for (item in alunos) {
                val nivelDoAluno: NivelDaMetrica? = item.nivelAtual
                if (nivelDoAluno != null && nivelDoAluno.id == nivel.id) {
                    quantidade = quantidade + 1
                }
            }
            if (quantidade > 0) {
                atalhos.add(AtalhoDeNivel(nivel, quantidade))
            }
        }
        return atalhos
    }

    fun podeSalvar(): Boolean {
        val camposPreenchidos: Boolean = descricao.isNotBlank() &&
            metricaId != null &&
            nivelAlvoId != null &&
            prazo != null
        return camposPreenchidos && quantidadeEscolhida() > 0 && !salvando && !carregando
    }
}

/**
 * Cria ou edita uma meta. Na criação, a professora escolhe a métrica; na edição a
 * métrica fica fixa (trocar de métrica seria outra meta).
 */
@HiltViewModel
class FormularioMetaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val metaRepository: MetaRepository,
    private val metricaRepository: MetricaRepository,
    private val alunoRepository: AlunoRepository,
    clock: Clock,
) : ViewModel() {

    private val rota: FormularioMetaRoute = savedStateHandle.toRoute<FormularioMetaRoute>()
    private val hoje: LocalDate = LocalDate.now(clock)

    // Lidos uma vez ao abrir: servem para remontar a lista quando a métrica muda.
    private var alunosDaTurma: List<Aluno> = emptyList()
    private var niveisDaTurma: List<NivelDaMetrica> = emptyList()
    private var resultadosDaTurma: List<ResultadoDatado> = emptyList()

    private val estadoMutavel = MutableStateFlow(
        FormularioMetaUiState(carregando = true, editando = rota.metaId != null, hoje = hoje)
    )
    val uiState: StateFlow<FormularioMetaUiState> = estadoMutavel

    init {
        viewModelScope.launch {
            carregar()
        }
    }

    private suspend fun carregar() {
        alunosDaTurma = alunoRepository.observarAlunosDaTurma(rota.turmaId).first()
        niveisDaTurma = metricaRepository.observarNiveisDaTurma(rota.turmaId).first()
        resultadosDaTurma = metricaRepository.observarResultadosDaTurma(rota.turmaId).first()
        val metricas: List<Metrica> = metricaRepository.observarMetricasDaTurma(rota.turmaId).first()

        var meta: Meta? = null
        var idsJaNaMeta: List<String> = emptyList()
        val metaId: String? = rota.metaId
        if (metaId != null) {
            meta = metaRepository.buscarMeta(metaId)
            idsJaNaMeta = idsDosAlunos(metaRepository.observarAlunosDaMeta(metaId).first())
        }

        val estadoInicial: FormularioMetaUiState = estadoMutavel.value.copy(carregando = false, metricas = metricas)
        if (meta == null) {
            val primeiraMetrica: Metrica? = metricas.firstOrNull()
            if (primeiraMetrica == null) {
                estadoMutavel.value = estadoInicial
            } else {
                estadoMutavel.value = montarParaMetrica(estadoInicial, primeiraMetrica.id, emptyList())
            }
            return
        }

        val comMetrica: FormularioMetaUiState = montarParaMetrica(estadoInicial, meta.metricaId, idsJaNaMeta)
        estadoMutavel.value = comMetrica.copy(
            descricao = meta.descricao,
            nivelAlvoId = meta.nivelAlvoId,
            prazo = meta.prazo,
        )
    }

    private fun idsDosAlunos(linhas: List<AlunoNaMeta>): List<String> {
        val ids = mutableListOf<String>()
        for (linha in linhas) {
            ids.add(linha.alunoId)
        }
        return ids
    }

    /**
     * Prepara níveis e lista de alunos para a métrica escolhida. O alvo começa no
     * nível mais avançado, que é o mais comum ("virar alfabéticos").
     */
    private fun montarParaMetrica(
        estado: FormularioMetaUiState,
        metricaId: String,
        idsEscolhidos: List<String>,
    ): FormularioMetaUiState {
        val niveis: List<NivelDaMetrica> = niveisDaMetrica(niveisDaTurma, metricaId)
        val nivelAtual: Map<String, ResultadoDatado> = nivelAtualDeCadaAluno(
            resultadosDaMetrica(resultadosDaTurma, metricaId),
            hoje,
        )

        val alunos = mutableListOf<AlunoParaEscolher>()
        for (aluno in alunosDaTurma) {
            val resultado: ResultadoDatado? = nivelAtual[aluno.id]
            var nivelDoAluno: NivelDaMetrica? = null
            if (resultado != null) {
                nivelDoAluno = buscarNivel(niveis, resultado.nivelId)
            }
            alunos.add(AlunoParaEscolher(aluno, nivelDoAluno, aluno.id in idsEscolhidos))
        }

        var nivelAlvoId: String? = null
        val maisAvancado: NivelDaMetrica? = niveis.lastOrNull()
        if (maisAvancado != null) {
            nivelAlvoId = maisAvancado.id
        }

        return estado.copy(metricaId = metricaId, niveis = niveis, nivelAlvoId = nivelAlvoId, alunos = alunos)
    }

    fun alterarDescricao(descricao: String) {
        estadoMutavel.value = estadoMutavel.value.copy(descricao = descricao)
    }

    /** Trocar de métrica desfaz a escolha de alunos: os níveis de antes não valem para a nova. */
    fun escolherMetrica(metricaId: String) {
        val estado: FormularioMetaUiState = estadoMutavel.value
        if (estado.editando || estado.metricaId == metricaId) {
            return
        }
        estadoMutavel.value = montarParaMetrica(estado, metricaId, emptyList())
    }

    fun escolherNivelAlvo(nivelId: String) {
        estadoMutavel.value = estadoMutavel.value.copy(nivelAlvoId = nivelId)
    }

    fun escolherPrazo(prazo: LocalDate) {
        estadoMutavel.value = estadoMutavel.value.copy(prazo = prazo)
    }

    fun alternarAluno(alunoId: String) {
        val novos = mutableListOf<AlunoParaEscolher>()
        for (item in estadoMutavel.value.alunos) {
            if (item.aluno.id == alunoId) {
                novos.add(item.copy(escolhido = !item.escolhido))
            } else {
                novos.add(item)
            }
        }
        estadoMutavel.value = estadoMutavel.value.copy(alunos = novos)
    }

    /** Escolhe exatamente quem está hoje no [nivelId] (e desmarca os demais). */
    fun escolherQuemEstaNoNivel(nivelId: String) {
        val novos = mutableListOf<AlunoParaEscolher>()
        for (item in estadoMutavel.value.alunos) {
            val nivelDoAluno: NivelDaMetrica? = item.nivelAtual
            val estaNoNivel: Boolean = nivelDoAluno != null && nivelDoAluno.id == nivelId
            novos.add(item.copy(escolhido = estaNoNivel))
        }
        estadoMutavel.value = estadoMutavel.value.copy(alunos = novos)
    }

    fun salvar() {
        val estado: FormularioMetaUiState = estadoMutavel.value
        if (!estado.podeSalvar()) {
            return
        }
        val metricaId: String = estado.metricaId ?: return
        val nivelAlvoId: String = estado.nivelAlvoId ?: return
        val prazo: LocalDate = estado.prazo ?: return
        estadoMutavel.value = estado.copy(salvando = true)

        val escolhidos = mutableListOf<AlunoEscolhidoParaMeta>()
        for (item in estado.alunos) {
            if (item.escolhido) {
                escolhidos.add(AlunoEscolhidoParaMeta(item.aluno.id, idDoNivel(item.nivelAtual)))
            }
        }

        viewModelScope.launch {
            val descricao: String = estado.descricao.trim()
            val metaId: String? = rota.metaId
            if (metaId == null) {
                metaRepository.criar(rota.turmaId, descricao, metricaId, nivelAlvoId, prazo, escolhidos)
            } else {
                metaRepository.editar(metaId, descricao, nivelAlvoId, prazo, escolhidos)
            }
            estadoMutavel.value = estadoMutavel.value.copy(salvando = false, salvo = true)
        }
    }

    private fun idDoNivel(nivel: NivelDaMetrica?): String? {
        if (nivel == null) {
            return null
        }
        return nivel.id
    }

    fun excluir() {
        val metaId: String = rota.metaId ?: return
        viewModelScope.launch {
            metaRepository.excluir(metaId)
            estadoMutavel.value = estadoMutavel.value.copy(excluida = true)
        }
    }
}
