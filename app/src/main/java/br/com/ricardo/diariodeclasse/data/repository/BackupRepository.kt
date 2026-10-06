package br.com.ricardo.diariodeclasse.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import br.com.ricardo.diariodeclasse.data.backup.ConversorDeBackup
import br.com.ricardo.diariodeclasse.data.backup.LeituraDoBackup
import br.com.ricardo.diariodeclasse.data.local.dao.BackupDao
import br.com.ricardo.diariodeclasse.data.local.entity.DadosDoDiario
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/**
 * Exportação e importação do diário inteiro. Trabalha com o texto do arquivo;
 * ler e gravar no local escolhido pela professora fica com `ArquivosDeBackup`.
 */
interface BackupRepository {
    /** O diário inteiro em JSON, pronto para gravar no arquivo. */
    suspend fun gerarExportacao(): String

    /** Valida o arquivo sem gravar nada: a professora ainda vai confirmar. */
    suspend fun lerArquivo(conteudo: String): LeituraDoBackup

    /** Apaga tudo o que está no app e grava [dados] no lugar (tudo ou nada). */
    suspend fun substituirTudo(dados: DadosDoDiario)

    /** `null` enquanto nada foi exportado neste aparelho. */
    fun observarUltimaExportacao(): Flow<Instant?>

    suspend fun registrarExportacao()
}

class BackupRepositoryImpl @Inject constructor(
    private val dao: BackupDao,
    private val dataStore: DataStore<Preferences>,
    private val clock: Clock,
) : BackupRepository {

    /**
     * Montar ou interpretar o JSON de um diário grande ocupa o processador;
     * `Dispatchers.Default` faz isso numa thread de cálculo, fora da thread da tela.
     * (A leitura do banco já roda fora da tela por conta do Room.)
     */
    override suspend fun gerarExportacao(): String {
        val dados: DadosDoDiario = dao.lerTudo()
        val exportadoEm: Instant = Instant.now(clock)
        return withContext(Dispatchers.Default) {
            ConversorDeBackup.gerarJson(dados, exportadoEm)
        }
    }

    override suspend fun lerArquivo(conteudo: String): LeituraDoBackup {
        return withContext(Dispatchers.Default) {
            ConversorDeBackup.lerJson(conteudo)
        }
    }

    override suspend fun substituirTudo(dados: DadosDoDiario) {
        dao.substituirTudo(dados)
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
