package br.com.ricardo.diariodeclasse.ui.chamada

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import br.com.ricardo.diariodeclasse.data.local.entity.Aluno
import br.com.ricardo.diariodeclasse.data.local.entity.RegistroPresenca
import br.com.ricardo.diariodeclasse.data.local.entity.Turma
import br.com.ricardo.diariodeclasse.data.repository.AlunoRepository
import br.com.ricardo.diariodeclasse.data.repository.ChamadaDoDia
import br.com.ricardo.diariodeclasse.data.repository.ChamadaRepository
import br.com.ricardo.diariodeclasse.data.repository.MarcacaoPresenca
import br.com.ricardo.diariodeclasse.data.repository.TurmaRepository
import br.com.ricardo.diariodeclasse.ui.navigation.ChamadaRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** Uma linha da tela: o aluno e o que a professora marcou para ele. */
data class AlunoNaChamada(
    val aluno: Aluno,
    val ausente: Boolean,
    val observacao: String,
)

sealed interface ChamadaUiState {
    data object Carregando : ChamadaUiState
    data object TurmaNaoEncontrada : ChamadaUiState

    data class Carregado(
        val turma: Turma,
        val data: LocalDate,
        val alunos: List<AlunoNaChamada>,
        /** `true` quando a chamada do dia já existia e a tela está editando. */
        val editando: Boolean,
        val salvando: Boolean = false,
        val salvo: Boolean = false,
    ) : ChamadaUiState {

        fun quantidadeDeAusentes(): Int {
            var quantidade = 0
            for (alunoNaChamada in alunos) {
                if (alunoNaChamada.ausente) {
                    quantidade = quantidade + 1
                }
            }
            return quantidade
        }
    }
}

/**
 * A tela é um formulário: carregamos os dados uma vez e as marcações ficam só na
 * memória até a professora tocar em "Salvar". Por isso usamos `first()` (leitura única)
 * em vez de observar o banco continuamente.
 */
@HiltViewModel
class ChamadaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val turmaRepository: TurmaRepository,
    private val alunoRepository: AlunoRepository,
    private val chamadaRepository: ChamadaRepository,
) : ViewModel() {

    private val rota: ChamadaRoute = savedStateHandle.toRoute<ChamadaRoute>()
    private val data: LocalDate = LocalDate.parse(rota.data)

    private val estadoMutavel = MutableStateFlow<ChamadaUiState>(ChamadaUiState.Carregando)
    val uiState: StateFlow<ChamadaUiState> = estadoMutavel

    init {
        viewModelScope.launch {
            carregar()
        }
    }

    private suspend fun carregar() {
        val turma: Turma? = turmaRepository.buscarTurma(rota.turmaId)
        if (turma == null) {
            estadoMutavel.value = ChamadaUiState.TurmaNaoEncontrada
            return
        }

        val alunos: List<Aluno> = alunoRepository.observarAlunosDaTurma(turma.id).first()
        val chamadaSalva: ChamadaDoDia? = chamadaRepository.buscarChamada(turma.id, data)

        val linhas = mutableListOf<AlunoNaChamada>()
        for (aluno in alunos) {
            linhas.add(criarLinha(aluno, chamadaSalva))
        }

        estadoMutavel.value = ChamadaUiState.Carregado(
            turma = turma,
            data = data,
            alunos = linhas,
            editando = chamadaSalva != null,
        )
    }

    /** Alunos sem registro (ex.: cadastrados depois da chamada) começam como presentes. */
    private fun criarLinha(aluno: Aluno, chamadaSalva: ChamadaDoDia?): AlunoNaChamada {
        val registro: RegistroPresenca? = buscarRegistroDoAluno(chamadaSalva, aluno.id)
        if (registro == null) {
            return AlunoNaChamada(aluno = aluno, ausente = false, observacao = "")
        }
        return AlunoNaChamada(
            aluno = aluno,
            ausente = !registro.presente,
            observacao = registro.observacao ?: "",
        )
    }

    private fun buscarRegistroDoAluno(chamadaSalva: ChamadaDoDia?, alunoId: String): RegistroPresenca? {
        if (chamadaSalva == null) {
            return null
        }
        for (registro in chamadaSalva.registros) {
            if (registro.alunoId == alunoId) {
                return registro
            }
        }
        return null
    }

    fun alternarPresenca(alunoId: String) {
        val linha: AlunoNaChamada = buscarLinha(alunoId) ?: return
        substituirLinha(linha.copy(ausente = !linha.ausente))
    }

    fun alterarObservacao(alunoId: String, observacao: String) {
        val linha: AlunoNaChamada = buscarLinha(alunoId) ?: return
        substituirLinha(linha.copy(observacao = observacao))
    }

    private fun buscarLinha(alunoId: String): AlunoNaChamada? {
        val estado: ChamadaUiState = estadoMutavel.value
        if (estado !is ChamadaUiState.Carregado) {
            return null
        }
        for (linha in estado.alunos) {
            if (linha.aluno.id == alunoId) {
                return linha
            }
        }
        return null
    }

    private fun substituirLinha(linhaAtualizada: AlunoNaChamada) {
        val estado: ChamadaUiState = estadoMutavel.value
        if (estado !is ChamadaUiState.Carregado) {
            return
        }
        val novasLinhas = mutableListOf<AlunoNaChamada>()
        for (linha in estado.alunos) {
            if (linha.aluno.id == linhaAtualizada.aluno.id) {
                novasLinhas.add(linhaAtualizada)
            } else {
                novasLinhas.add(linha)
            }
        }
        estadoMutavel.value = estado.copy(alunos = novasLinhas)
    }

    fun salvar() {
        val estado: ChamadaUiState = estadoMutavel.value
        if (estado !is ChamadaUiState.Carregado || estado.salvando) {
            return
        }
        estadoMutavel.value = estado.copy(salvando = true)

        val marcacoes = mutableListOf<MarcacaoPresenca>()
        for (linha in estado.alunos) {
            marcacoes.add(criarMarcacao(linha))
        }

        viewModelScope.launch {
            chamadaRepository.salvar(estado.turma.id, estado.data, marcacoes)
            estadoMutavel.value = estado.copy(salvando = false, salvo = true)
        }
    }

    /**
     * Observação só vale para falta: se a professora escreveu algo e depois
     * desmarcou a falta, o texto é descartado.
     */
    private fun criarMarcacao(linha: AlunoNaChamada): MarcacaoPresenca {
        val observacaoLimpa: String = linha.observacao.trim()
        val observacao: String?
        if (linha.ausente && observacaoLimpa.isNotEmpty()) {
            observacao = observacaoLimpa
        } else {
            observacao = null
        }
        return MarcacaoPresenca(
            alunoId = linha.aluno.id,
            presente = !linha.ausente,
            observacao = observacao,
        )
    }
}
