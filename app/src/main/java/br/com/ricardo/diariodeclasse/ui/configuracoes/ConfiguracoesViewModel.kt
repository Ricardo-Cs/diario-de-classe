package br.com.ricardo.diariodeclasse.ui.configuracoes

import android.database.SQLException
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.ricardo.diariodeclasse.data.backup.ArquivosDeBackup
import br.com.ricardo.diariodeclasse.data.backup.LeituraDoBackup
import br.com.ricardo.diariodeclasse.data.backup.MotivoDeArquivoInvalido
import br.com.ricardo.diariodeclasse.data.backup.ResumoDoBackup
import br.com.ricardo.diariodeclasse.data.backup.resumirBackup
import br.com.ricardo.diariodeclasse.data.local.entity.DadosDoDiario
import br.com.ricardo.diariodeclasse.data.repository.BackupRepository
import br.com.ricardo.diariodeclasse.data.repository.Exportacao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

/** Quando foi a última exportação, para incentivar a professora a exportar de tempos em tempos. */
sealed interface UltimaExportacao {
    data object Carregando : UltimaExportacao
    data object Nunca : UltimaExportacao
    data class Em(val data: LocalDate) : UltimaExportacao
}

/**
 * Arquivo já lido e validado, esperando a professora confirmar a substituição.
 * As fotos do arquivo já foram copiadas para [pastaDasFotos], à parte das atuais.
 */
data class ImportacaoPendente(
    val dados: DadosDoDiario,
    val resumo: ResumoDoBackup,
    val exportadoEm: LocalDate,
    val pastaDasFotos: File,
)

/** Avisos de resultado, mostrados uma vez no rodapé da tela. */
enum class MensagemDaTela {
    EXPORTADO,
    IMPORTADO,
    ERRO_AO_GRAVAR,
    ERRO_AO_LER,
    NAO_E_BACKUP,
    VERSAO_MAIS_NOVA,
    ARQUIVO_DANIFICADO,
    ERRO_AO_IMPORTAR,

    /** Os dados entraram, mas as fotos não puderam ser movidas para o lugar. */
    FOTOS_NAO_IMPORTADAS,
}

data class ConfiguracoesUiState(
    val hoje: LocalDate,
    val ultimaExportacao: UltimaExportacao,
    /** Exportando ou importando: os botões ficam desabilitados. */
    val ocupado: Boolean,
    val importacaoPendente: ImportacaoPendente?,
    val mensagem: MensagemDaTela?,
)

/** A parte do estado que muda pelas ações da tela (o resto vem do DataStore). */
private data class EstadoDasOperacoes(
    val ocupado: Boolean = false,
    val importacaoPendente: ImportacaoPendente? = null,
    val mensagem: MensagemDaTela? = null,
)

@HiltViewModel
class ConfiguracoesViewModel @Inject constructor(
    private val backupRepository: BackupRepository,
    private val arquivos: ArquivosDeBackup,
    private val clock: Clock,
) : ViewModel() {

    private val hoje: LocalDate = LocalDate.now(clock)
    private val operacoes = MutableStateFlow(EstadoDasOperacoes())

    val uiState: StateFlow<ConfiguracoesUiState> = combine(
        backupRepository.observarUltimaExportacao(),
        operacoes,
    ) { instante, estadoDasOperacoes ->
        ConfiguracoesUiState(
            hoje = hoje,
            ultimaExportacao = ultimaExportacaoDe(instante),
            ocupado = estadoDasOperacoes.ocupado,
            importacaoPendente = estadoDasOperacoes.importacaoPendente,
            mensagem = estadoDasOperacoes.mensagem,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ConfiguracoesUiState(
            hoje = hoje,
            ultimaExportacao = UltimaExportacao.Carregando,
            ocupado = false,
            importacaoPendente = null,
            mensagem = null,
        ),
    )

    private fun ultimaExportacaoDe(instante: Instant?): UltimaExportacao {
        if (instante == null) {
            return UltimaExportacao.Nunca
        }
        return UltimaExportacao.Em(instante.atZone(clock.zone).toLocalDate())
    }

    /** Ex.: "diario-2026-10-06.zip". A tela usa como nome sugerido no "Salvar como". */
    fun dataParaNomeDoArquivo(): String {
        return hoje.toString()
    }

    fun exportarPara(destino: Uri) {
        if (operacoes.value.ocupado) {
            return
        }
        operacoes.value = operacoes.value.copy(ocupado = true)

        viewModelScope.launch {
            val resultado: MensagemDaTela = exportar(destino)
            terminarComMensagem(resultado)
        }
    }

    /** `SecurityException`: o Android pode revogar o acesso ao local escolhido. */
    private suspend fun exportar(destino: Uri): MensagemDaTela {
        try {
            val exportacao: Exportacao = backupRepository.gerarExportacao()
            arquivos.gravar(destino, exportacao.json, exportacao.fotos)
            backupRepository.registrarExportacao()
            return MensagemDaTela.EXPORTADO
        } catch (erro: IOException) {
            return MensagemDaTela.ERRO_AO_GRAVAR
        } catch (erro: SecurityException) {
            return MensagemDaTela.ERRO_AO_GRAVAR
        }
    }

    /** Lê e valida o arquivo. Se estiver tudo certo, a tela pede confirmação antes de gravar. */
    fun lerArquivoParaImportar(origem: Uri) {
        if (operacoes.value.ocupado) {
            return
        }
        operacoes.value = operacoes.value.copy(ocupado = true)

        viewModelScope.launch {
            prepararImportacao(origem)
        }
    }

    private suspend fun prepararImportacao(origem: Uri) {
        val pastaDasFotos: File = backupRepository.prepararPastaDeImportacao()
        val conteudo: String? = lerConteudo(origem, pastaDasFotos)
        if (conteudo == null) {
            backupRepository.descartarImportacao(pastaDasFotos)
            terminarComMensagem(MensagemDaTela.ERRO_AO_LER)
            return
        }

        val leitura: LeituraDoBackup = backupRepository.lerArquivo(conteudo)
        when (leitura) {
            is LeituraDoBackup.Invalida -> {
                backupRepository.descartarImportacao(pastaDasFotos)
                terminarComMensagem(mensagemDoMotivo(leitura.motivo))
            }
            is LeituraDoBackup.Valida -> {
                val pendente = ImportacaoPendente(
                    dados = leitura.dados,
                    resumo = resumirBackup(leitura.dados),
                    exportadoEm = leitura.exportadoEm.atZone(clock.zone).toLocalDate(),
                    pastaDasFotos = pastaDasFotos,
                )
                operacoes.value = operacoes.value.copy(ocupado = false, importacaoPendente = pendente)
            }
        }
    }

    /** `null` quando o arquivo não pôde ser aberto (ou é um .zip danificado). */
    private suspend fun lerConteudo(origem: Uri, pastaDasFotos: File): String? {
        try {
            return arquivos.ler(origem, pastaDasFotos)
        } catch (erro: IOException) {
            return null
        } catch (erro: SecurityException) {
            return null
        }
    }

    /**
     * A gravação é uma transação: se o arquivo tiver uma referência quebrada, o
     * banco recusa (`SQLException`) e nada do que estava no app é perdido.
     */
    fun confirmarImportacao() {
        val pendente: ImportacaoPendente = operacoes.value.importacaoPendente ?: return
        operacoes.value = operacoes.value.copy(ocupado = true, importacaoPendente = null)

        viewModelScope.launch {
            val resultado: MensagemDaTela = importar(pendente)
            terminarComMensagem(resultado)
        }
    }

    private suspend fun importar(pendente: ImportacaoPendente): MensagemDaTela {
        try {
            backupRepository.substituirTudo(pendente.dados, pendente.pastaDasFotos)
            return MensagemDaTela.IMPORTADO
        } catch (erro: SQLException) {
            return MensagemDaTela.ERRO_AO_IMPORTAR
        } catch (erro: IOException) {
            return MensagemDaTela.FOTOS_NAO_IMPORTADAS
        }
    }

    fun cancelarImportacao() {
        val pendente: ImportacaoPendente = operacoes.value.importacaoPendente ?: return
        operacoes.value = operacoes.value.copy(importacaoPendente = null)
        viewModelScope.launch {
            backupRepository.descartarImportacao(pendente.pastaDasFotos)
        }
    }

    /** A tela avisa que já mostrou a mensagem, para ela não aparecer de novo. */
    fun mensagemExibida() {
        operacoes.value = operacoes.value.copy(mensagem = null)
    }

    private fun terminarComMensagem(mensagem: MensagemDaTela) {
        operacoes.value = operacoes.value.copy(ocupado = false, mensagem = mensagem)
    }

    private fun mensagemDoMotivo(motivo: MotivoDeArquivoInvalido): MensagemDaTela {
        return when (motivo) {
            MotivoDeArquivoInvalido.NAO_E_BACKUP_DO_APP -> MensagemDaTela.NAO_E_BACKUP
            MotivoDeArquivoInvalido.VERSAO_MAIS_NOVA -> MensagemDaTela.VERSAO_MAIS_NOVA
            MotivoDeArquivoInvalido.ARQUIVO_DANIFICADO -> MensagemDaTela.ARQUIVO_DANIFICADO
        }
    }
}
