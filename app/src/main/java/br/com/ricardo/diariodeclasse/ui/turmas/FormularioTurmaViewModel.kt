package br.com.ricardo.diariodeclasse.ui.turmas

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import br.com.ricardo.diariodeclasse.data.local.entity.Periodo
import br.com.ricardo.diariodeclasse.data.local.entity.Turma
import br.com.ricardo.diariodeclasse.data.repository.TurmaRepository
import br.com.ricardo.diariodeclasse.ui.navigation.FormularioTurmaRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

data class FormularioTurmaUiState(
    val nome: String,
    val anoSerie: String,
    val periodo: Periodo,
    val anoLetivo: String,
    val editando: Boolean,
    val salvando: Boolean = false,
    val salvo: Boolean = false,
    /** A turma foi excluída: a tela volta direto para a lista de turmas. */
    val excluida: Boolean = false,
) {
    fun podeSalvar(): Boolean {
        return nome.isNotBlank() && anoLetivoEhValido() && !salvando
    }

    private fun anoLetivoEhValido(): Boolean {
        val ano: Int? = anoLetivo.toIntOrNull()
        if (ano == null) {
            return false
        }
        return ano in 2000..2100
    }
}

/** Serve tanto para criar quanto para editar: depende de a rota trazer um `turmaId`. */
@HiltViewModel
class FormularioTurmaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val turmaRepository: TurmaRepository,
    clock: Clock,
) : ViewModel() {

    private val rota: FormularioTurmaRoute = savedStateHandle.toRoute<FormularioTurmaRoute>()
    private var turmaEmEdicao: Turma? = null

    private val estadoMutavel = MutableStateFlow(
        FormularioTurmaUiState(
            nome = "",
            anoSerie = "",
            periodo = Periodo.MANHA,
            anoLetivo = LocalDate.now(clock).year.toString(),
            editando = rota.turmaId != null,
        )
    )
    val uiState: StateFlow<FormularioTurmaUiState> = estadoMutavel

    init {
        val turmaId: String? = rota.turmaId
        if (turmaId != null) {
            viewModelScope.launch {
                carregarTurma(turmaId)
            }
        }
    }

    private suspend fun carregarTurma(turmaId: String) {
        val turma: Turma = turmaRepository.buscarTurma(turmaId) ?: return
        turmaEmEdicao = turma
        estadoMutavel.value = estadoMutavel.value.copy(
            nome = turma.nome,
            anoSerie = turma.anoSerie,
            periodo = turma.periodo,
            anoLetivo = turma.anoLetivo.toString(),
        )
    }

    fun alterarNome(nome: String) {
        estadoMutavel.value = estadoMutavel.value.copy(nome = nome)
    }

    fun alterarAnoSerie(anoSerie: String) {
        estadoMutavel.value = estadoMutavel.value.copy(anoSerie = anoSerie)
    }

    fun alterarPeriodo(periodo: Periodo) {
        estadoMutavel.value = estadoMutavel.value.copy(periodo = periodo)
    }

    fun alterarAnoLetivo(texto: String) {
        val somenteDigitos: String = texto.filter { caractere -> caractere.isDigit() }
        val noMaximoQuatroDigitos: String = somenteDigitos.take(4)
        estadoMutavel.value = estadoMutavel.value.copy(anoLetivo = noMaximoQuatroDigitos)
    }

    fun salvar() {
        val estado: FormularioTurmaUiState = estadoMutavel.value
        if (!estado.podeSalvar()) {
            return
        }
        estadoMutavel.value = estado.copy(salvando = true)

        viewModelScope.launch {
            val nome: String = estado.nome.trim()
            val anoSerie: String = estado.anoSerie.trim()
            val anoLetivo: Int = estado.anoLetivo.toInt()
            val turmaExistente: Turma? = turmaEmEdicao

            if (turmaExistente == null) {
                turmaRepository.criar(nome, anoSerie, estado.periodo, anoLetivo)
            } else {
                val turmaAlterada = turmaExistente.copy(
                    nome = nome,
                    anoSerie = anoSerie,
                    periodo = estado.periodo,
                    anoLetivo = anoLetivo,
                )
                turmaRepository.atualizar(turmaAlterada)
            }

            estadoMutavel.value = estadoMutavel.value.copy(salvando = false, salvo = true)
        }
    }

    /** Soft delete: a turma e os alunos dela deixam de aparecer, mas nada é apagado do banco. */
    fun excluir() {
        val turma: Turma = turmaEmEdicao ?: return
        viewModelScope.launch {
            turmaRepository.excluir(turma.id)
            estadoMutavel.value = estadoMutavel.value.copy(excluida = true)
        }
    }
}
