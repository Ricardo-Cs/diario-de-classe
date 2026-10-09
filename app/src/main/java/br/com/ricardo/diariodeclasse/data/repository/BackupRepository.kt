package br.com.ricardo.diariodeclasse.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import android.database.SQLException
import br.com.ricardo.diariodeclasse.data.backup.ConversorDeBackup
import br.com.ricardo.diariodeclasse.data.backup.LeituraDoBackup
import br.com.ricardo.diariodeclasse.data.fotos.ArquivosDeFotos
import br.com.ricardo.diariodeclasse.data.local.dao.BackupDao
import br.com.ricardo.diariodeclasse.data.local.entity.DadosDoDiario
import br.com.ricardo.diariodeclasse.data.local.entity.Foto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/** O que vai no .zip: o diário em JSON e as imagens das fotos. */
data class Exportacao(
    val json: String,
    val fotos: List<File>,
)

/**
 * Exportação e importação do diário inteiro. Trabalha com o conteúdo do arquivo;
 * ler e gravar no local escolhido pela professora fica com `ArquivosDeBackup`.
 */
interface BackupRepository {
    /** O diário inteiro, pronto para gravar no arquivo. */
    suspend fun gerarExportacao(): Exportacao

    /** Valida o arquivo sem gravar nada: a professora ainda vai confirmar. */
    suspend fun lerArquivo(conteudo: String): LeituraDoBackup

    /** Pasta vazia que recebe as fotos do arquivo enquanto a professora não confirma. */
    suspend fun prepararPastaDeImportacao(): File

    /**
     * Apaga tudo o que está no app e grava [dados] no lugar (tudo ou nada); as
     * fotos atuais são trocadas pelas de [pastaDasFotos].
     */
    suspend fun substituirTudo(dados: DadosDoDiario, pastaDasFotos: File)

    /** A professora desistiu da importação (ou o arquivo era inválido). */
    suspend fun descartarImportacao(pastaDasFotos: File)

    /** `null` enquanto nada foi exportado neste aparelho. */
    fun observarUltimaExportacao(): Flow<Instant?>

    suspend fun registrarExportacao()
}

class BackupRepositoryImpl @Inject constructor(
    private val dao: BackupDao,
    private val arquivosDeFotos: ArquivosDeFotos,
    private val dataStore: DataStore<Preferences>,
    private val clock: Clock,
) : BackupRepository {

    /**
     * Montar ou interpretar o JSON de um diário grande ocupa o processador;
     * `Dispatchers.Default` faz isso numa thread de cálculo, fora da thread da tela.
     * (A leitura do banco já roda fora da tela por conta do Room.)
     */
    override suspend fun gerarExportacao(): Exportacao {
        val dados: DadosDoDiario = dao.lerTudo()
        val exportadoEm: Instant = Instant.now(clock)
        val json: String = withContext(Dispatchers.Default) {
            ConversorDeBackup.gerarJson(dados, exportadoEm)
        }
        val fotos: List<File> = withContext(Dispatchers.IO) {
            imagensParaExportar(dados.fotos)
        }
        return Exportacao(json, fotos)
    }

    /**
     * Só as fotos que aparecem no app. Uma foto pode estar no banco sem a imagem
     * (ex.: banco recuperado do backup automático, que não leva as fotos); nesse
     * caso o arquivo simplesmente não vai no .zip.
     */
    private fun imagensParaExportar(fotos: List<Foto>): List<File> {
        val imagens = mutableListOf<File>()
        for (foto in fotos) {
            val imagem: File = arquivosDeFotos.arquivoDa(foto.nomeDoArquivo)
            if (foto.deletedAt == null && imagem.exists()) {
                imagens.add(imagem)
            }
        }
        return imagens
    }

    override suspend fun lerArquivo(conteudo: String): LeituraDoBackup {
        return withContext(Dispatchers.Default) {
            ConversorDeBackup.lerJson(conteudo)
        }
    }

    override suspend fun prepararPastaDeImportacao(): File {
        return arquivosDeFotos.prepararPastaDeImportacao()
    }

    /**
     * O banco vem primeiro: se ele recusar o arquivo (`SQLException`), as fotos
     * atuais também ficam como estavam.
     */
    override suspend fun substituirTudo(dados: DadosDoDiario, pastaDasFotos: File) {
        try {
            dao.substituirTudo(dados)
        } catch (erro: SQLException) {
            arquivosDeFotos.descartarImportacao(pastaDasFotos)
            throw erro
        }
        arquivosDeFotos.substituirPelaImportacao(pastaDasFotos)
    }

    override suspend fun descartarImportacao(pastaDasFotos: File) {
        arquivosDeFotos.descartarImportacao(pastaDasFotos)
    }

    /** O DataStore guarda o instante como número (milissegundos desde 1970). */
    override fun observarUltimaExportacao(): Flow<Instant?> {
        return dataStore.data.map { preferencias ->
            val milissegundos: Long? = preferencias[CHAVE_ULTIMA_EXPORTACAO]
            if (milissegundos == null) {
                null
            } else {
                Instant.ofEpochMilli(milissegundos)
            }
        }
    }

    override suspend fun registrarExportacao() {
        val agora: Instant = Instant.now(clock)
        dataStore.edit { preferencias ->
            preferencias[CHAVE_ULTIMA_EXPORTACAO] = agora.toEpochMilli()
        }
    }

    companion object {
        private val CHAVE_ULTIMA_EXPORTACAO = longPreferencesKey("ultima_exportacao")
    }
}
