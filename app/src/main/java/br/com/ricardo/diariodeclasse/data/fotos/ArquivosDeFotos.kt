package br.com.ricardo.diariodeclasse.data.fotos

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject

/**
 * Os arquivos de imagem das fotos. Ficam em `files/fotos/`, a pasta privada do app:
 * outros apps não enxergam, e as fotos não aparecem na galeria do celular.
 *
 * É uma interface para os testes do repositório usarem uma versão em memória.
 */
interface ArquivosDeFotos {
    fun arquivoDa(nomeDoArquivo: String): File

    /** Reduz, corrige a rotação e grava em JPEG. Lança `IOException` se a imagem não puder ser lida. */
    suspend fun salvarReduzida(origem: Uri, nomeDoArquivo: String)

    suspend fun apagar(nomeDoArquivo: String)

    /** Arquivo vazio para a câmera gravar a foto, e o endereço dele para entregar à câmera. */
    fun criarArquivoDaCamera(): ArquivoDaCamera
    fun arquivoDaCamera(nomeTemporario: String): File

    /** Pasta vazia onde a importação coloca as fotos do arquivo antes da confirmação. */
    suspend fun prepararPastaDeImportacao(): File

    /** Troca todas as fotos atuais pelas da pasta de importação. */
    suspend fun substituirPelaImportacao(pastaImportada: File)

    suspend fun descartarImportacao(pastaImportada: File)
}

/**
 * [uri] é o endereço que a câmera recebe para gravar a foto; [nomeTemporario]
 * identifica o arquivo depois, mesmo se o Android fechar o app enquanto a
 * câmera está aberta.
 */
data class ArquivoDaCamera(val nomeTemporario: String, val uri: Uri)

class ArquivosDeFotosImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : ArquivosDeFotos {

    private val pastaDasFotos: File = File(context.filesDir, PASTA_DAS_FOTOS)
    private val pastaDaCamera: File = File(context.cacheDir, PASTA_DA_CAMERA)

    override fun arquivoDa(nomeDoArquivo: String): File {
        return File(pastaDasFotos, nomeDoArquivo)
    }

    /**
     * Grava primeiro num arquivo ".tmp" e só no fim troca o nome: se algo der
     * errado no meio, não sobra um JPEG pela metade com o nome da foto.
     */
    override suspend fun salvarReduzida(origem: Uri, nomeDoArquivo: String) {
        withContext(Dispatchers.IO) {
            val bitmap: Bitmap = lerReduzida(origem)
            val rotacao: Int = lerRotacao(origem)
            val ajustada: Bitmap = ajustarTamanhoERotacao(bitmap, rotacao)

            pastaDasFotos.mkdirs()
            val temporario = File(pastaDasFotos, "$nomeDoArquivo.tmp")
            FileOutputStream(temporario).use { saida ->
                ajustada.compress(Bitmap.CompressFormat.JPEG, QUALIDADE_DO_JPEG, saida)
            }
            // `recycle` devolve a memória dos pixels na hora, sem esperar o coletor de lixo.
            // `!==` compara se são o mesmo objeto (o `==` de objetos no Java).
            if (ajustada !== bitmap) {
                ajustada.recycle()
            }
            bitmap.recycle()

            val destino: File = arquivoDa(nomeDoArquivo)
            if (!temporario.renameTo(destino)) {
                temporario.delete()
                throw IOException("Não foi possível gravar a foto")
            }
        }
    }

    /**
     * Lê o arquivo duas vezes: a primeira só para saber o tamanho
     * (`inJustDecodeBounds` não carrega os pixels), a segunda já reduzindo.
     */
    private fun lerReduzida(origem: Uri): Bitmap {
        val somenteTamanho = BitmapFactory.Options()
        somenteTamanho.inJustDecodeBounds = true
        abrir(origem).use { entrada ->
            BitmapFactory.decodeStream(entrada, null, somenteTamanho)
        }
        if (somenteTamanho.outWidth <= 0 || somenteTamanho.outHeight <= 0) {
            throw IOException("O arquivo não é uma imagem")
        }

        val tamanhoOriginal = TamanhoDaFoto(somenteTamanho.outWidth, somenteTamanho.outHeight)
        val opcoes = BitmapFactory.Options()
        opcoes.inSampleSize = calcularReducaoNaLeitura(tamanhoOriginal, LADO_MAXIMO_DA_FOTO)

        val bitmap: Bitmap? = abrir(origem).use { entrada ->
            BitmapFactory.decodeStream(entrada, null, opcoes)
        }
        if (bitmap == null) {
            throw IOException("Não foi possível ler a imagem")
        }
        return bitmap
    }

    /** Imagens sem EXIF (ex.: capturas de tela) não precisam girar. */
    private fun lerRotacao(origem: Uri): Int {
        try {
            val orientacao: Int = abrir(origem).use { entrada ->
                val exif = ExifInterface(entrada)
                exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            }
            return rotacaoDaOrientacao(orientacao)
        } catch (erro: IOException) {
            return 0
        }
    }

    /**
     * Uma única transformação (escala + rotação) gera a imagem final. Se não
     * houver nada a mudar, devolve a própria [bitmap].
     */
    private fun ajustarTamanhoERotacao(bitmap: Bitmap, rotacao: Int): Bitmap {
        val tamanhoLido = TamanhoDaFoto(bitmap.width, bitmap.height)
        val tamanhoDesejado: TamanhoDaFoto = tamanhoFinal(tamanhoLido, LADO_MAXIMO_DA_FOTO)
        if (tamanhoDesejado == tamanhoLido && rotacao == 0) {
            return bitmap
        }

        val transformacao = Matrix()
        val escala: Float = tamanhoDesejado.largura.toFloat() / tamanhoLido.largura
        transformacao.postScale(escala, escala)
        transformacao.postRotate(rotacao.toFloat())
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, transformacao, true)
    }

    private fun abrir(origem: Uri): InputStream {
        val entrada: InputStream? = context.contentResolver.openInputStream(origem)
        if (entrada == null) {
            throw IOException("Não foi possível abrir a imagem")
        }
        return entrada
    }

    override suspend fun apagar(nomeDoArquivo: String) {
        withContext(Dispatchers.IO) {
            arquivoDa(nomeDoArquivo).delete()
        }
    }

    /**
     * A câmera é outro app e não pode gravar na pasta privada deste. O
     * `FileProvider` cria um endereço `content://` que dá a ela permissão de
     * escrita só neste arquivo (configurado no AndroidManifest e em
     * `res/xml/caminhos_compartilhados.xml`).
     */
    override fun criarArquivoDaCamera(): ArquivoDaCamera {
        pastaDaCamera.mkdirs()
        val arquivo: File = File.createTempFile("camera-", ".jpg", pastaDaCamera)
        val autoridade = "${context.packageName}.arquivos"
        val uri: Uri = FileProvider.getUriForFile(context, autoridade, arquivo)
        return ArquivoDaCamera(nomeTemporario = arquivo.name, uri = uri)
    }

    override fun arquivoDaCamera(nomeTemporario: String): File {
        return File(pastaDaCamera, nomeTemporario)
    }

    override suspend fun prepararPastaDeImportacao(): File {
        return withContext(Dispatchers.IO) {
            val pasta = File(context.filesDir, PASTA_DA_IMPORTACAO)
            pasta.deleteRecursively()
            pasta.mkdirs()
            pasta
        }
    }

    /**
     * Renomear uma pasta é instantâneo e não copia nada: as duas estão em
     * `files/`, no mesmo armazenamento.
     */
    override suspend fun substituirPelaImportacao(pastaImportada: File) {
        withContext(Dispatchers.IO) {
            pastaDasFotos.deleteRecursively()
            if (!pastaImportada.renameTo(pastaDasFotos)) {
                throw IOException("Não foi possível mover as fotos importadas")
            }
        }
    }

    override suspend fun descartarImportacao(pastaImportada: File) {
        withContext(Dispatchers.IO) {
            pastaImportada.deleteRecursively()
        }
    }

    companion object {
        private const val PASTA_DAS_FOTOS = "fotos"
        private const val PASTA_DA_IMPORTACAO = "fotos-importadas"
        private const val PASTA_DA_CAMERA = "camera"
    }
}
