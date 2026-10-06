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
import br.com.ricardo.diariodeclasse.data.repository.PendenciaRepository
import br.com.ricardo.diariodeclasse.data.repository.TurmaRepository
import br.com.ricardo.diariodeclasse.ui.navigation.ChamadaRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** Um ausente da chamada salva, a quem a professora pode atribuir atividades. */
data class AusenteDaChamada(
    val aluno: Aluno,
    val registroPresencaId: String,
    /** Já tem pendência ligada a esta falta (ex.: chamada editada depois). */
    val jaTemPendencia: Boolean,
)

/** Resumo de uma atividade já registrada no painel, para a professora ver o que fez. */
data class AtividadeAdicionada(
    val descricao: String,
    val quantidadeDeAlunos: Int,
)

/**
 * Em que passo a tela está:
 * - marcando as faltas;
 * - chamada salva, oferecendo registrar atividades para os ausentes;
 * - concluída (a tela deve fechar).
 */
sealed interface EtapaDaChamada {
    data object Marcando : EtapaDaChamada
    data class OferecendoPendencias(
        val ausentes: List<AusenteDaChamada>,
        val adicionadas: List<AtividadeAdicionada>,
    ) : EtapaDaChamada
    data object Concluida : EtapaDaChamada
}

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
        val etapa: EtapaDaChamada = EtapaDaChamada.Marcando,
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
    private val pendenciaRepository: PendenciaRepository,
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
        if (estado.etapa !is EtapaDaChamada.Marcando) {
            return
        }
        estadoMutavel.value = estado.copy(salvando = true)

        val marcacoes = mutableListOf<MarcacaoPresenca>()
        for (linha in estado.alunos) {
            marcacoes.add(criarMarcacao(linha))
        }

        viewModelScope.launch {
            val chamadaSalva: ChamadaDoDia = chamadaRepository.salvar(estado.turma.id, estado.data, marcacoes)
            val ausentes: List<AusenteDaChamada> = montarAusentes(estado.alunos, chamadaSalva)

            val proximaEtapa: EtapaDaChamada
            if (ausentes.isEmpty()) {
                proximaEtapa = EtapaDaChamada.Concluida
            } else {
                proximaEtapa = EtapaDaChamada.OferecendoPendencias(ausentes = ausentes, adicionadas = emptyList())
            }
            estadoMutavel.value = estado.copy(salvando = false, etapa = proximaEtapa)
        }
    }

    /** Liga cada aluno que faltou ao id do registro de falta que acabou de ser gravado. */
    private suspend fun montarAusentes(linhas: List<AlunoNaChamada>, chamadaSalva: ChamadaDoDia): List<AusenteDaChamada> {
        val faltas = mutableListOf<RegistroPresenca>()
        for (linha in linhas) {
            if (!linha.ausente) {
                continue
            }
            val registro: RegistroPresenca? = buscarRegistroDoAluno(chamadaSalva, linha.aluno.id)
            if (registro != null) {
                faltas.add(registro)
            }
        }

        val idsDasFaltas = mutableListOf<String>()
        for (falta in faltas) {
            idsDasFaltas.add(falta.id)
        }
        val faltasComPendencia: List<String> = pendenciaRepository.buscarFaltasComPendencia(idsDasFaltas)

        val ausentes = mutableListOf<AusenteDaChamada>()
        for (linha in linhas) {
            val registro: RegistroPresenca? = buscarRegistroDoAluno(chamadaSalva, linha.aluno.id)
            if (linha.ausente && registro != null) {
                ausentes.add(
                    AusenteDaChamada(
                        aluno = linha.aluno,
                        registroPresencaId = registro.id,
                        jaTemPendencia = registro.id in faltasComPendencia,
                    )
                )
            }
        }
        return ausentes
    }

    /**
     * Cria a mesma atividade para cada ausente escolhido, ligada à falta dele.
     * O painel continua aberto para a professora registrar outra atividade, se quiser.
     */
    fun adicionarAtividadeParaAusentes(descricao: String, registroPresencaIds: List<String>, dataLembrete: LocalDate) {
        val estado: ChamadaUiState = estadoMutavel.value
        if (estado !is ChamadaUiState.Carregado) {
            return
        }
        val etapa: EtapaDaChamada = estado.etapa
        if (etapa !is EtapaDaChamada.OferecendoPendencias) {
            return
        }
        val descricaoLimpa: String = descricao.trim()
        if (descricaoLimpa.isEmpty() || registroPresencaIds.isEmpty()) {
            return
        }

        viewModelScope.launch {
            for (ausente in etapa.ausentes) {
                if (ausente.registroPresencaId in registroPresencaIds) {
                    pendenciaRepository.criar(
                        alunoId = ausente.aluno.id,
                        descricao = descricaoLimpa,
                        dataLembrete = dataLembrete,
                        registroPresencaId = ausente.registroPresencaId,
                    )
                }
            }
            registrarAtividadeAdicionada(AtividadeAdicionada(descricaoLimpa, registroPresencaIds.size))
        }
    }

    /** Relê o estado atual: outra atividade pode ter sido adicionada enquanto esta gravava. */
    private fun registrarAtividadeAdicionada(adicionada: AtividadeAdicionada) {
        val estadoAtual: ChamadaUiState = estadoMutavel.value
        if (estadoAtual !is ChamadaUiState.Carregado) {
            return
        }
        val etapaAtual: EtapaDaChamada = estadoAtual.etapa
        if (etapaAtual !is EtapaDaChamada.OferecendoPendencias) {
            return
        }
        val novaEtapa = etapaAtual.copy(adicionadas = etapaAtual.adicionadas + adicionada)
        estadoMutavel.value = estadoAtual.copy(etapa = novaEtapa)
    }

    /** "Pular", "Concluir" ou fechar o painel: a chamada já está salva, então a tela pode fechar. */
    fun concluir() {
        val estado: ChamadaUiState = estadoMutavel.value
        if (estado !is ChamadaUiState.Carregado) {
            return
        }
        estadoMutavel.value = estado.copy(etapa = EtapaDaChamada.Concluida)
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
