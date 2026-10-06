package br.com.ricardo.diariodeclasse.ui.alunos

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.ricardo.diariodeclasse.data.local.entity.Aluno
import br.com.ricardo.diariodeclasse.data.local.entity.Anotacao
import br.com.ricardo.diariodeclasse.data.local.entity.FaltaDoAluno
import br.com.ricardo.diariodeclasse.data.local.entity.PendenciaComOrigem
import br.com.ricardo.diariodeclasse.data.repository.AlunoRepository
import br.com.ricardo.diariodeclasse.data.repository.AnotacaoRepository
import br.com.ricardo.diariodeclasse.data.repository.ChamadaRepository
import br.com.ricardo.diariodeclasse.data.repository.PendenciaRepository
import br.com.ricardo.diariodeclasse.ui.navigation.AlunoNoInicioRoute
import br.com.ricardo.diariodeclasse.ui.navigation.AlunoRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

sealed interface AlunoUiState {
    data object Carregando : AlunoUiState
    data object AlunoNaoEncontrado : AlunoUiState

    data class Carregado(
        val aluno: Aluno,
        val hoje: LocalDate,
        /** Não entregues, das que esperam há mais tempo para as mais novas. */
        val pendencias: List<PendenciaComOrigem>,
        val faltas: ResumoDasFaltas,
        /** Mais recentes primeiro. */
        val anotacoes: List<Anotacao>,
    ) : AlunoUiState
}

/**
 * Tela do aluno, a "página do caderno" dele: pendências, faltas e anotações
 * juntas, mais a edição do cadastro.
 */
@HiltViewModel
class AlunoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val alunoRepository: AlunoRepository,
    private val anotacaoRepository: AnotacaoRepository,
    private val pendenciaRepository: PendenciaRepository,
    chamadaRepository: ChamadaRepository,
    private val clock: Clock,
) : ViewModel() {

    /**
     * A tela tem duas rotas ([AlunoRoute] na aba Turmas e [AlunoNoInicioRoute] no
     * Início), e as duas guardam o id com o mesmo nome, "alunoId". Lemos direto
     * pela chave para servir a qualquer uma delas.
     */
    private val alunoId: String = lerAlunoIdDaRota(savedStateHandle)

    val uiState: StateFlow<AlunoUiState> = combine(
        alunoRepository.observarAluno(alunoId),
        pendenciaRepository.observarPendentesDoAluno(alunoId),
        chamadaRepository.observarFaltasDoAluno(alunoId),
        anotacaoRepository.observarDoAluno(alunoId),
    ) { aluno, pendencias, faltas, anotacoes ->
        criarEstado(aluno, pendencias, faltas, anotacoes)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AlunoUiState.Carregando,
    )

    private fun criarEstado(
        aluno: Aluno?,
        pendencias: List<PendenciaComOrigem>,
        faltas: List<FaltaDoAluno>,
        anotacoes: List<Anotacao>,
    ): AlunoUiState {
        if (aluno == null) {
            return AlunoUiState.AlunoNaoEncontrado
        }
        val hoje: LocalDate = LocalDate.now(clock)
        return AlunoUiState.Carregado(
            aluno = aluno,
            hoje = hoje,
            pendencias = pendencias,
            faltas = calcularResumoDasFaltas(faltas, hoje),
            anotacoes = anotacoes,
        )
    }

    private fun lerAlunoIdDaRota(savedStateHandle: SavedStateHandle): String {
        val id: String? = savedStateHandle.get<String>("alunoId")
        if (id == null) {
            throw IllegalStateException("A tela do aluno foi aberta sem o id do aluno")
        }
        return id
    }

    /** A anotação nova recebe a data de hoje. */
    fun criarAnotacao(texto: String) {
        val textoLimpo: String = texto.trim()
        if (textoLimpo.isEmpty()) {
            return
        }
        viewModelScope.launch {
            anotacaoRepository.criar(alunoId, textoLimpo, LocalDate.now(clock))
        }
    }

    fun editarAnotacao(anotacaoId: String, texto: String) {
        val textoLimpo: String = texto.trim()
        if (textoLimpo.isEmpty()) {
            return
        }
        viewModelScope.launch {
            anotacaoRepository.editarTexto(anotacaoId, textoLimpo)
        }
    }

    fun excluirAnotacao(anotacaoId: String) {
        viewModelScope.launch {
            anotacaoRepository.excluir(anotacaoId)
        }
    }

    fun restaurarAnotacao(anotacaoId: String) {
        viewModelScope.launch {
            anotacaoRepository.restaurar(anotacaoId)
        }
    }

    /** Pendência avulsa criada na própria tela do aluno (sem vínculo com falta). */
    fun criarPendencia(descricao: String, dataLembrete: LocalDate) {
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

    fun marcarPendenciaComoEntregue(pendenciaId: String) {
        viewModelScope.launch {
            pendenciaRepository.marcarComoEntregue(pendenciaId)
        }
    }

    fun desfazerEntregaDaPendencia(pendenciaId: String) {
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

    fun renomearAluno(aluno: Aluno, novoNome: String) {
        val nomeSemEspacosNasPontas: String = novoNome.trim()
        if (nomeSemEspacosNasPontas.isEmpty()) {
            return
        }
        viewModelScope.launch {
            alunoRepository.renomear(aluno, nomeSemEspacosNasPontas)
        }
    }

    /** Depois de excluir, o Flow do aluno emite `null` e a tela volta sozinha. */
    fun excluirAluno() {
        viewModelScope.launch {
            alunoRepository.excluir(alunoId)
        }
    }
}
