package br.com.ricardo.diariodeclasse.data.backup

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

/**
 * Lê e grava o arquivo no lugar que a professora escolheu no seletor de arquivos
 * do Android (Downloads, Google Drive...). O seletor devolve um [Uri], uma espécie
 * de endereço do arquivo; o `contentResolver` abre esse endereço.
 *
 * O app não precisa de permissão de armazenamento: o acesso vale só para o arquivo
 * escolhido, e quem o concede é o próprio seletor (Storage Access Framework).
 *
 * `withContext(Dispatchers.IO)` tira a leitura/gravação da thread da tela, como um
 * `await` em operação de I/O no Node, para a interface não travar.
 */
class ArquivosDeBackup @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /** Grava o .zip com o JSON e as imagens das [fotos]. */
    suspend fun gravar(destino: Uri, json: String, fotos: List<File>) {
        withContext(Dispatchers.IO) {
            // "wt" = escrita, apagando o conteúdo anterior se o arquivo já existir.
            val saida: OutputStream? = context.contentResolver.openOutputStream(destino, "wt")
            if (saida == null) {
                throw IOException("Não foi possível abrir o arquivo para gravação")
            }
            // `use` fecha o arquivo ao terminar, mesmo se der erro
            // (como o try-with-resources do Java).
            saida.use { arquivo ->
                PacoteDeBackup.escrever(arquivo, json, fotos)
            }
        }
    }

    /**
     * Devolve o texto do JSON. Se o arquivo for um .zip, as fotos dele são
     * copiadas para [pastaDasFotos]. Também aceita um .json avulso, exportado
     * pelas versões do app anteriores às fotos.
     *
     * Um .zip sem o JSON devolve texto vazio, que a leitura do backup recusa
     * como "não é uma exportação do diário".
     */
    suspend fun ler(origem: Uri, pastaDasFotos: File): String {
        return withContext(Dispatchers.IO) {
            val entrada: InputStream? = context.contentResolver.openInputStream(origem)
            if (entrada == null) {
                throw IOException("Não foi possível abrir o arquivo para leitura")
            }
            BufferedInputStream(entrada).use { arquivo ->
                lerConteudo(arquivo, pastaDasFotos)
            }
        }
    }

    private fun lerConteudo(arquivo: BufferedInputStream, pastaDasFotos: File): String {
        if (!PacoteDeBackup.eZip(arquivo)) {
            return String(arquivo.readBytes(), Charsets.UTF_8)
        }
        val json: String? = PacoteDeBackup.ler(arquivo, pastaDasFotos)
        if (json == null) {
            return ""
        }
        return json
    }
}
