package br.com.ricardo.diariodeclasse.ui.configuracoes

import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.ricardo.diariodeclasse.R
import br.com.ricardo.diariodeclasse.data.backup.ResumoDoBackup
import br.com.ricardo.diariodeclasse.ui.componentes.BarraSuperior
import br.com.ricardo.diariodeclasse.ui.componentes.DialogoConfirmacao
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Tipos de arquivo aceitos na importação: o .zip atual e o .json das versões
 * anteriores. Alguns gerenciadores de arquivos e o Google Drive marcam o arquivo
 * com outro tipo (zip "do Windows", texto ou "binário genérico").
 */
private val TIPOS_ACEITOS_NA_IMPORTACAO: Array<String> = arrayOf(
    "application/zip",
    "application/x-zip-compressed",
    "application/json",
    "text/plain",
    "application/octet-stream",
)

@Composable
fun ConfiguracoesScreen(
    aoVoltar: () -> Unit,
    viewModel: ConfiguracoesViewModel = hiltViewModel(),
) {
    val estado: ConfiguracoesUiState = viewModel.uiState.collectAsStateWithLifecycle().value
    val avisos: SnackbarHostState = remember { SnackbarHostState() }

    /*
     * "Launchers" abrem telas do sistema e recebem o resultado de volta (o antigo
     * `startActivityForResult`). Aqui, o seletor de arquivos do Android:
     * - CreateDocument: "Salvar como", devolve o endereço do arquivo criado;
     * - OpenDocument: "Abrir", devolve o endereço do arquivo escolhido.
     * Se a professora cancelar, o resultado é `null`.
     */
    val seletorParaExportar: ManagedActivityResultLauncher<String, Uri?> = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
    ) { destino: Uri? ->
        if (destino != null) {
            viewModel.exportarPara(destino)
        }
    }
    val seletorParaImportar: ManagedActivityResultLauncher<Array<String>, Uri?> = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { origem: Uri? ->
        if (origem != null) {
            viewModel.lerArquivoParaImportar(origem)
        }
    }

    val nomeSugerido: String = stringResource(R.string.mais_nome_do_arquivo, viewModel.dataParaNomeDoArquivo())
    MostrarMensagem(mensagem = estado.mensagem, avisos = avisos, aoExibir = { viewModel.mensagemExibida() })

    Scaffold(
        topBar = { BarraSuperior(titulo = stringResource(R.string.configuracoes_titulo), aoVoltar = aoVoltar) },
        snackbarHost = { SnackbarHost(avisos) },
    ) { espacamentoDasBarras ->
        Column(
            modifier = Modifier
                .padding(espacamentoDasBarras)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            if (estado.ocupado) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Text(
                text = stringResource(R.string.mais_seus_dados),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
            )

            OpcaoDeDados(
                icone = R.drawable.ic_exportar,
                titulo = stringResource(R.string.mais_exportar),
                descricao = stringResource(R.string.mais_exportar_descricao),
                detalhe = textoDaUltimaExportacao(estado),
                habilitada = !estado.ocupado,
                aoTocar = { seletorParaExportar.launch(nomeSugerido) },
            )
            OpcaoDeDados(
                icone = R.drawable.ic_importar,
                titulo = stringResource(R.string.mais_importar),
                descricao = stringResource(R.string.mais_importar_descricao),
                detalhe = null,
                habilitada = !estado.ocupado,
                aoTocar = { seletorParaImportar.launch(TIPOS_ACEITOS_NA_IMPORTACAO) },
            )

            Avisos()
        }
    }

    val pendente: ImportacaoPendente? = estado.importacaoPendente
    if (pendente != null) {
        DialogoConfirmacao(
            titulo = stringResource(R.string.importar_confirmar_titulo),
            mensagem = stringResource(
                R.string.importar_confirmar_mensagem,
                pendente.exportadoEm.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                textoDoResumo(pendente.resumo),
            ),
            textoConfirmar = stringResource(R.string.importar_confirmar),
            aoConfirmar = { viewModel.confirmarImportacao() },
            aoCancelar = { viewModel.cancelarImportacao() },
        )
    }
}

/**
 * Mostra o aviso no rodapé quando o ViewModel publica uma mensagem nova.
 * `LaunchedEffect(mensagem)` roda de novo sempre que a mensagem muda, como um
 * `useEffect` com dependência no React.
 *
 * A ordem importa: `showSnackbar` espera o aviso sumir e só depois a mensagem é
 * limpa. Limpar antes mudaria a chave do efeito, e o Compose cancelaria o aviso
 * no meio da exibição.
 */
@Composable
private fun MostrarMensagem(mensagem: MensagemDaTela?, avisos: SnackbarHostState, aoExibir: () -> Unit) {
    val texto: String? = textoDaMensagem(mensagem)

    LaunchedEffect(mensagem) {
        if (texto != null) {
            avisos.showSnackbar(texto)
            aoExibir()
        }
    }
}

@Composable
private fun OpcaoDeDados(
    icone: Int,
    titulo: String,
    descricao: String,
    detalhe: String?,
    habilitada: Boolean,
    aoTocar: () -> Unit,
) {
    ListItem(
        leadingContent = {
            Icon(
                painter = painterResource(icone),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
            )
        },
        headlineContent = { Text(titulo) },
        supportingContent = {
            Column {
                Text(descricao)
                if (detalhe != null) {
                    Text(
                        text = detalhe,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        },
        modifier = Modifier.clickable(enabled = habilitada, onClick = aoTocar),
    )
}

@Composable
private fun Avisos() {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(16.dp),
    ) {
        Text(
            text = stringResource(R.string.mais_aviso_privacidade),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.mais_aviso_backup_automatico),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** "Última exportação: hoje", "há 12 dias" ou "Ainda não exportado neste celular". */
@Composable
private fun textoDaUltimaExportacao(estado: ConfiguracoesUiState): String? {
    val ultima: UltimaExportacao = estado.ultimaExportacao
    when (ultima) {
        is UltimaExportacao.Carregando -> return null
        is UltimaExportacao.Nunca -> return stringResource(R.string.mais_ultima_exportacao_nunca)
        is UltimaExportacao.Em -> {
            val dias: Int = ChronoUnit.DAYS.between(ultima.data, estado.hoje).toInt()
            if (dias <= 0) {
                return stringResource(R.string.mais_ultima_exportacao_hoje)
            }
            if (dias == 1) {
                return stringResource(R.string.mais_ultima_exportacao_ontem)
            }
            return pluralStringResource(R.plurals.mais_ultima_exportacao_dias, dias, dias)
        }
    }
}

/** "3 turmas, 74 alunos, 120 chamadas, 5 pendências em aberto, 52 anotações, 4 sondagens e 30 fotos". */
@Composable
private fun textoDoResumo(resumo: ResumoDoBackup): String {
    val turmas: String = pluralStringResource(R.plurals.importar_turmas, resumo.turmas, resumo.turmas)
    val alunos: String = pluralStringResource(R.plurals.importar_alunos, resumo.alunos, resumo.alunos)
    val chamadas: String = pluralStringResource(R.plurals.importar_chamadas, resumo.chamadas, resumo.chamadas)
    val pendencias: String = pluralStringResource(
        R.plurals.importar_pendencias,
        resumo.pendenciasEmAberto,
        resumo.pendenciasEmAberto,
    )
    val anotacoes: String = pluralStringResource(R.plurals.importar_anotacoes, resumo.anotacoes, resumo.anotacoes)
    val sondagens: String = pluralStringResource(R.plurals.importar_sondagens, resumo.sondagens, resumo.sondagens)
    val fotos: String = pluralStringResource(R.plurals.importar_fotos, resumo.fotos, resumo.fotos)

    val inicio = "$turmas, $alunos, $chamadas, $pendencias, $anotacoes, $sondagens"
    return stringResource(R.string.importar_lista_e, inicio, fotos)
}

@Composable
private fun textoDaMensagem(mensagem: MensagemDaTela?): String? {
    if (mensagem == null) {
        return null
    }
    val idDoTexto: Int = when (mensagem) {
        MensagemDaTela.EXPORTADO -> R.string.mensagem_exportado
        MensagemDaTela.IMPORTADO -> R.string.mensagem_importado
        MensagemDaTela.ERRO_AO_GRAVAR -> R.string.mensagem_erro_ao_gravar
        MensagemDaTela.ERRO_AO_LER -> R.string.mensagem_erro_ao_ler
        MensagemDaTela.NAO_E_BACKUP -> R.string.mensagem_nao_e_backup
        MensagemDaTela.VERSAO_MAIS_NOVA -> R.string.mensagem_versao_mais_nova
        MensagemDaTela.ARQUIVO_DANIFICADO -> R.string.mensagem_arquivo_danificado
        MensagemDaTela.ERRO_AO_IMPORTAR -> R.string.mensagem_erro_ao_importar
        MensagemDaTela.FOTOS_NAO_IMPORTADAS -> R.string.mensagem_fotos_nao_importadas
    }
    return stringResource(idDoTexto)
}
