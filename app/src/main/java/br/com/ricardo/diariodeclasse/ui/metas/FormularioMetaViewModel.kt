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

/** Como a professora acompanha a meta. Escolhida na criação e fixa depois. */
enum class FormaDeAcompanhar {
    /** Ela marca na tela da meta quem atingiu. */
    MARCANDO_A_MAO,

    /** O progresso vem das sondagens de uma métrica de níveis. */
    POR_METRICA,
}

/** Um aluno da turma na lista de escolha. [nivelAtual] só é preenchido nas metas por métrica. */
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
    val forma: FormaDeAcompanhar = FormaDeAcompanhar.MARCANDO_A_MAO,
    val metricas: List<Metrica> = emptyList(),
    val metricaId: String? = null,
    /** Níveis da métrica escolhida, do mais inicial ao mais avançado. Vazio na meta à mão. */
    val niveis: List<NivelDaMetrica> = emptyList(),
    val nivelAlvoId: String? = null,
    /** Opcional. */
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

    /** Um atalho por nível que tem alguém hoje. Só existe nas metas por métrica. */
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
        if (salvando || carregando) {
            return false
        }
        if (descricao.isBlank() || quantidadeEscolhida() == 0) {
            return false
        }
        if (forma == FormaDeAcompanhar.POR_METRICA) {
            return metricaId != null && nivelAlvoId != null
        }
        return true
    }
}

/**
 * Cria ou edita uma meta. Na criação, a professora escolhe como acompanhar (à mão
 * ou por métrica); na edição a forma e a métrica ficam fixas.
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

    // Lidos uma vez ao abrir: servem para remontar a lista quando a forma ou a métrica mudam.
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

        // Meta nova: começa marcada à mão, a forma mais simples.
        if (meta == null) {
            estadoMutavel.value = montarSemMetrica(estadoInicial, emptyList())
            return
        }

        val metricaDaMeta: String? = meta.metricaId
        val montado: FormularioMetaUiState
        if (metricaDaMeta == null) {
            montado = montarSemMetrica(estadoInicial, idsJaNaMeta)
        } else {
            montado = montarParaMetrica(estadoInicial, metricaDaMeta, idsJaNaMeta).copy(nivelAlvoId = meta.nivelAlvoId)
        }
        estadoMutavel.value = montado.copy(descricao = meta.descricao, prazo = meta.prazo)
    }

    private fun idsDosAlunos(linhas: List<AlunoNaMeta>): List<String> {
        val ids = mutableListOf<String>()
        for (linha in linhas) {
            ids.add(linha.alunoId)
        }
        return ids
    }

    private fun idsEscolhidos(alunos: List<AlunoParaEscolher>): List<String> {
        val ids = mutableListOf<String>()
        for (item in alunos) {
            if (item.escolhido) {
                ids.add(item.aluno.id)
            }
        }
        return ids
    }

    /** Lista de alunos sem níveis, para a meta marcada à mão. */
    private fun montarSemMetrica(estado: FormularioMetaUiState, idsEscolhidos: List<String>): FormularioMetaUiState {
        val alunos = mutableListOf<AlunoParaEscolher>()
        for (aluno in alunosDaTurma) {
            alunos.add(AlunoParaEscolher(aluno, nivelAtual = null, escolhido = aluno.id in idsEscolhidos))
        }
        return estado.copy(
            forma = FormaDeAcompanhar.MARCANDO_A_MAO,
            metricaId = null,
            niveis = emptyList(),
            nivelAlvoId = null,
            alunos = alunos,
        )
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

        return estado.copy(
            forma = FormaDeAcompanhar.POR_METRICA,
            metricaId = metricaId,
            niveis = niveis,
            nivelAlvoId = nivelAlvoId,
            alunos = alunos,
        )
    }

    fun alterarDescricao(descricao: String) {
        estadoMutavel.value = estadoMutavel.value.copy(descricao = descricao)
    }

    /**
     * Só na criação. Os alunos já escolhidos continuam escolhidos; ao passar para
     * "por métrica", a primeira métrica da turma vem selecionada.
     */
    fun escolherForma(forma: FormaDeAcompanhar) {
        val estado: FormularioMetaUiState = estadoMutavel.value
        if (estado.editando || estado.forma == forma) {
            return
        }
        val escolhidos: List<String> = idsEscolhidos(estado.alunos)

        if (forma == FormaDeAcompanhar.MARCANDO_A_MAO) {
            estadoMutavel.value = montarSemMetrica(estado, escolhidos)
            return
        }
        val primeiraMetrica: Metrica = estado.metricas.firstOrNull() ?: return
        estadoMutavel.value = montarParaMetrica(estado, primeiraMetrica.id, escolhidos)
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

    fun removerPrazo() {
        estadoMutavel.value = estadoMutavel.value.copy(prazo = null)
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

    fun escolherTodaATurma() {
        val novos = mutableListOf<AlunoParaEscolher>()
        for (item in estadoMutavel.value.alunos) {
            novos.add(item.copy(escolhido = true))
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
        estadoMutavel.value = estado.copy(salvando = true)

        // Na meta à mão, métrica e nível-alvo vão `null`; o nível inicial também.
        val porMetrica: Boolean = estado.forma == FormaDeAcompanhar.POR_METRICA
        var metricaId: String? = null
        var nivelAlvoId: String? = null
        if (porMetrica) {
            metricaId = estado.metricaId
            nivelAlvoId = estado.nivelAlvoId
        }

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
                metaRepository.criar(rota.turmaId, descricao, metricaId, nivelAlvoId, estado.prazo, escolhidos)
            } else {
                metaRepository.editar(metaId, descricao, nivelAlvoId, estado.prazo, escolhidos)
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
