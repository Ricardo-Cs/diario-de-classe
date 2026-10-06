package br.com.ricardo.diariodeclasse.data.backup

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

    suspend fun gravar(destino: Uri, conteudo: String) {
        withContext(Dispatchers.IO) {
            // "wt" = escrita, apagando o conteúdo anterior se o arquivo já existir.
            val saida: OutputStream? = context.contentResolver.openOutputStream(destino, "wt")
            if (saida == null) {
                throw IOException("Não foi possível abrir o arquivo para gravação")
            }
            // `use` fecha o arquivo ao terminar, mesmo se der erro
            // (como o try-with-resources do Java).
            saida.use { arquivo ->
                arquivo.write(conteudo.toByteArray(Charsets.UTF_8))
            }
        }
    }

    suspend fun ler(origem: Uri): String {
        return withContext(Dispatchers.IO) {
            val entrada: InputStream? = context.contentResolver.openInputStream(origem)
            if (entrada == null) {
                throw IOException("Não foi possível abrir o arquivo para leitura")
            }
            entrada.use { arquivo ->
                String(arquivo.readBytes(), Charsets.UTF_8)
            }
        }
    }
}
