package br.com.ricardo.diariodeclasse.ui.metricas

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import br.com.ricardo.diariodeclasse.data.local.entity.Metrica
import br.com.ricardo.diariodeclasse.data.local.entity.NivelDaMetrica
import br.com.ricardo.diariodeclasse.data.repository.MetricaRepository
import br.com.ricardo.diariodeclasse.data.repository.NivelEditado
import br.com.ricardo.diariodeclasse.ui.navigation.FormularioMetricaRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/** Um nível como aparece no formulário. */
data class NivelNoFormulario(
    /**
     * Identifica a linha na tela enquanto a professora edita (o Compose usa como
     * `key` da lista). Nível já gravado usa o próprio id; nível novo ganha um
     * UUID provisório, que não vai para o banco.
     */
    val chave: String,
    /** `null` = nível novo, ainda não gravado. */
    val id: String?,
    val nome: String,
    /** Tem alunos avaliados ou é usado por uma meta: pode ser renomeado, mas não removido. */
    val emUso: Boolean,
)

data class FormularioMetricaUiState(
    val nome: String,
    val niveis: List<NivelNoFormulario>,
    val editando: Boolean,
    val carregando: Boolean,
    val salvando: Boolean = false,
    val salvo: Boolean = false,
    val excluida: Boolean = false,
) {
    /** Uma escala precisa de pelo menos dois degraus, todos com nome. */
    fun escalaEhValida(): Boolean {
        if (niveis.size < 2) {
            return false
        }
        for (nivel in niveis) {
            if (nivel.nome.isBlank()) {
                return false
            }
        }
        return true
    }

    fun podeSalvar(): Boolean {
        return nome.isNotBlank() && escalaEhValida() && !salvando && !carregando
    }

    fun temNivelEmUso(): Boolean {
        for (nivel in niveis) {
            if (nivel.emUso) {
                return true
            }
        }
        return false
    }
}

/** Serve para criar e editar, como o formulário de turma: depende de a rota trazer um `metricaId`. */
@HiltViewModel
class FormularioMetricaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val metricaRepository: MetricaRepository,
) : ViewModel() {

    private val rota: FormularioMetricaRoute = savedStateHandle.toRoute<FormularioMetricaRoute>()

    private val estadoMutavel = MutableStateFlow(
        FormularioMetricaUiState(
            nome = "",
            niveis = listOf(nivelNovo(""), nivelNovo("")),
            editando = rota.metricaId != null,
            carregando = rota.metricaId != null,
        )
    )
    val uiState: StateFlow<FormularioMetricaUiState> = estadoMutavel

    init {
        val metricaId: String? = rota.metricaId
        if (metricaId != null) {
            viewModelScope.launch {
                carregarMetrica(metricaId)
            }
        }
    }

    private suspend fun carregarMetrica(metricaId: String) {
        val metrica: Metrica = metricaRepository.buscarMetrica(metricaId) ?: return
        val niveis: List<NivelDaMetrica> = metricaRepository.observarNiveis(metricaId).first()
        val idsEmUso: List<String> = metricaRepository.buscarIdsDeNiveisEmUso(metricaId)

        val linhas = mutableListOf<NivelNoFormulario>()
        for (nivel in niveis) {
            linhas.add(
                NivelNoFormulario(chave = nivel.id, id = nivel.id, nome = nivel.nome, emUso = nivel.id in idsEmUso)
            )
        }
        estadoMutavel.value = estadoMutavel.value.copy(nome = metrica.nome, niveis = linhas, carregando = false)
    }

    private fun nivelNovo(nome: String): NivelNoFormulario {
        return NivelNoFormulario(chave = UUID.randomUUID().toString(), id = null, nome = nome, emUso = false)
    }

    fun alterarNome(nome: String) {
        estadoMutavel.value = estadoMutavel.value.copy(nome = nome)
    }

    /**
     * Preenche nome e níveis com o modelo (os textos vêm dos recursos da tela).
     * Só é oferecido na criação, quando não há níveis gravados a perder.
     */
    fun usarModelo(nome: String, niveis: List<String>) {
        val linhas = mutableListOf<NivelNoFormulario>()
        for (nomeDoNivel in niveis) {
            linhas.add(nivelNovo(nomeDoNivel))
        }
        estadoMutavel.value = estadoMutavel.value.copy(nome = nome, niveis = linhas)
    }

    fun alterarNomeDoNivel(chave: String, nome: String) {
        val novos = mutableListOf<NivelNoFormulario>()
        for (nivel in estadoMutavel.value.niveis) {
            if (nivel.chave == chave) {
                novos.add(nivel.copy(nome = nome))
            } else {
                novos.add(nivel)
            }
        }
        estadoMutavel.value = estadoMutavel.value.copy(niveis = novos)
    }

    /** O nível novo entra no fim: normalmente é um degrau mais avançado. */
    fun adicionarNivel() {
        val novos: List<NivelNoFormulario> = estadoMutavel.value.niveis + nivelNovo("")
        estadoMutavel.value = estadoMutavel.value.copy(niveis = novos)
    }

    fun removerNivel(chave: String) {
        val novos = mutableListOf<NivelNoFormulario>()
        for (nivel in estadoMutavel.value.niveis) {
            val ehORemovido: Boolean = nivel.chave == chave && !nivel.emUso
            if (!ehORemovido) {
                novos.add(nivel)
            }
        }
        estadoMutavel.value = estadoMutavel.value.copy(niveis = novos)
    }

    fun subirNivel(chave: String) {
        val posicao: Int = posicaoDoNivel(chave)
        if (posicao <= 0) {
            return
        }
        trocarDePosicao(posicao, posicao - 1)
    }

    fun descerNivel(chave: String) {
        val posicao: Int = posicaoDoNivel(chave)
        val ultimaPosicao: Int = estadoMutavel.value.niveis.size - 1
        if (posicao < 0 || posicao >= ultimaPosicao) {
            return
        }
        trocarDePosicao(posicao, posicao + 1)
    }

    private fun posicaoDoNivel(chave: String): Int {
        val niveis: List<NivelNoFormulario> = estadoMutavel.value.niveis
        for (posicao in niveis.indices) {
            if (niveis[posicao].chave == chave) {
                return posicao
            }
        }
        return -1
    }

    private fun trocarDePosicao(posicaoA: Int, posicaoB: Int) {
        val novos: MutableList<NivelNoFormulario> = estadoMutavel.value.niveis.toMutableList()
        val nivelA: NivelNoFormulario = novos[posicaoA]
        novos[posicaoA] = novos[posicaoB]
        novos[posicaoB] = nivelA
        estadoMutavel.value = estadoMutavel.value.copy(niveis = novos)
    }

    fun salvar() {
        val estado: FormularioMetricaUiState = estadoMutavel.value
        if (!estado.podeSalvar()) {
            return
        }
        estadoMutavel.value = estado.copy(salvando = true)

        val niveis = mutableListOf<NivelEditado>()
        for (nivel in estado.niveis) {
            niveis.add(NivelEditado(id = nivel.id, nome = nivel.nome.trim()))
        }

        viewModelScope.launch {
            metricaRepository.salvarMetrica(
                metricaId = rota.metricaId,
                turmaId = rota.turmaId,
                nome = estado.nome.trim(),
                niveis = niveis,
            )
            estadoMutavel.value = estadoMutavel.value.copy(salvando = false, salvo = true)
        }
    }

    fun excluir() {
        val metricaId: String = rota.metricaId ?: return
        viewModelScope.launch {
            metricaRepository.excluirMetrica(metricaId)
            estadoMutavel.value = estadoMutavel.value.copy(excluida = true)
        }
    }
}
