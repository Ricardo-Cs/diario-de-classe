package br.com.ricardo.diariodeclasse.data.backup

import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * O arquivo exportado é um .zip com:
 * - `diario.json`: os dados (ver ArquivoDeBackup);
 * - `fotos/<nome>.jpg`: as imagens das fotos.
 *
 * Não depende de Android (só `java.util.zip`), então é testado com testes comuns.
 * Tudo é lido e gravado aos poucos (streams): o .zip pode ter centenas de MB e
 * não caberia inteiro na memória.
 */
object PacoteDeBackup {

    private const val NOME_DO_JSON = "diario.json"
    private const val PASTA_DAS_FOTOS = "fotos/"

    /**
     * JPEG já é comprimido; tentar comprimir de novo só gasta tempo. As fotos vão
     * sem compressão e o JSON (texto, que comprime bem) com a compressão padrão.
     */
    fun escrever(saida: OutputStream, json: String, fotos: List<File>) {
        val zip = ZipOutputStream(saida)

        zip.setLevel(Deflater.DEFAULT_COMPRESSION)
        zip.putNextEntry(ZipEntry(NOME_DO_JSON))
        zip.write(json.toByteArray(Charsets.UTF_8))
        zip.closeEntry()

        zip.setLevel(Deflater.NO_COMPRESSION)
        for (foto in fotos) {
            zip.putNextEntry(ZipEntry(PASTA_DAS_FOTOS + foto.name))
            FileInputStream(foto).use { entrada ->
                entrada.copyTo(zip)
            }
            zip.closeEntry()
        }

        // `finish` grava o índice do zip no fim do arquivo, sem fechar a [saida],
        // que é de quem a abriu.
        zip.finish()
    }

    /**
     * Copia as fotos para [pastaDasFotos] e devolve o texto do JSON, ou `null` se
     * o .zip não tiver o `diario.json` (então não é uma exportação deste app).
     */
    fun ler(entrada: InputStream, pastaDasFotos: File): String? {
        var json: String? = null
        val zip = ZipInputStream(entrada)

        var item: ZipEntry? = zip.nextEntry
        while (item != null) {
            if (item.name == NOME_DO_JSON) {
                json = String(zip.readBytes(), Charsets.UTF_8)
            } else {
                copiarSeForFoto(item, zip, pastaDasFotos)
            }
            item = zip.nextEntry
        }
        return json
    }

    /**
     * Só aceita "fotos/<nome simples>". Um .zip malicioso pode ter itens como
     * "fotos/../../banco.db" para gravar fora da pasta ("Zip Slip"); nomes com
     * barra ou começando com ponto são ignorados.
     */
    private fun copiarSeForFoto(item: ZipEntry, zip: ZipInputStream, pastaDasFotos: File) {
        if (item.isDirectory || !item.name.startsWith(PASTA_DAS_FOTOS)) {
            return
        }
        val nome: String = item.name.substring(PASTA_DAS_FOTOS.length)
        if (!nomeDeFotoSeguro(nome)) {
            return
        }
        FileOutputStream(File(pastaDasFotos, nome)).use { saida ->
            zip.copyTo(saida)
        }
    }

    fun nomeDeFotoSeguro(nome: String): Boolean {
        if (nome.isEmpty() || nome.startsWith(".")) {
            return false
        }
        if (nome.contains("/") || nome.contains("\\")) {
            return false
        }
        return true
    }

    /**
     * Todo .zip começa com as letras "PK" (iniciais do criador do formato). Lê os
     * dois primeiros bytes e volta ao início (`mark`/`reset`), para a leitura de
     * verdade começar do zero.
     */
    fun eZip(entrada: BufferedInputStream): Boolean {
        entrada.mark(2)
        val primeiro: Int = entrada.read()
        val segundo: Int = entrada.read()
        entrada.reset()
        return primeiro == 'P'.code && segundo == 'K'.code
    }
}
