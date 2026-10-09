package br.com.ricardo.diariodeclasse.data.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class PacoteDeBackupTest {
    /** Pasta temporária do JUnit, apagada ao fim de cada teste. */
    @get:Rule
    val pastas = TemporaryFolder()

    private val json = """{"formato": "diario-de-classe", "versao": 5}"""

    @Test
    fun escreverELer_devolveOJsonECopiaAsFotos() {
        val fotoA: File = pastas.newFile("a.jpg")
        fotoA.writeBytes(byteArrayOf(1, 2, 3))
        val fotoB: File = pastas.newFile("b.jpg")
        fotoB.writeBytes(byteArrayOf(4, 5))
        val pacote = ByteArrayOutputStream()

        PacoteDeBackup.escrever(pacote, json, listOf(fotoA, fotoB))
        val destino: File = pastas.newFolder("importadas")
        val lido: String? = PacoteDeBackup.ler(ByteArrayInputStream(pacote.toByteArray()), destino)

        assertEquals(json, lido)
        assertArrayEquals(byteArrayOf(1, 2, 3), File(destino, "a.jpg").readBytes())
        assertArrayEquals(byteArrayOf(4, 5), File(destino, "b.jpg").readBytes())
    }

    @Test
    fun zipSemOJson_devolveNulo() {
        val pacote: ByteArray = zipCom("outra-coisa.txt", byteArrayOf(1))

        val lido: String? = PacoteDeBackup.ler(ByteArrayInputStream(pacote), pastas.newFolder("importadas"))

        assertNull(lido)
    }

    /** "Zip Slip": um item que tenta sair da pasta das fotos não pode ser gravado. */
    @Test
    fun itemComCaminhoParaForaDaPasta_eIgnorado() {
        val pacote: ByteArray = zipCom("fotos/../../invasor.jpg", byteArrayOf(9))
        val destino: File = pastas.newFolder("dentro", "importadas")

        PacoteDeBackup.ler(ByteArrayInputStream(pacote), destino)

        assertFalse(File(pastas.root, "invasor.jpg").exists())
        assertFalse(File(pastas.root, "dentro/invasor.jpg").exists())
        assertEquals(0, destino.listFiles()!!.size)
    }

    @Test
    fun nomeDeFotoSeguro_aceitaSoNomesSimples() {
        assertTrue(PacoteDeBackup.nomeDeFotoSeguro("3f2a-c1.jpg"))
        assertFalse(PacoteDeBackup.nomeDeFotoSeguro(""))
        assertFalse(PacoteDeBackup.nomeDeFotoSeguro(".."))
        assertFalse(PacoteDeBackup.nomeDeFotoSeguro("../banco.db"))
        assertFalse(PacoteDeBackup.nomeDeFotoSeguro("sub/pasta.jpg"))
        assertFalse(PacoteDeBackup.nomeDeFotoSeguro("..\\banco.db"))
    }

    @Test
    fun eZip_reconheceZipEJsonSemConsumirOInicio() {
        val zip = BufferedInputStream(ByteArrayInputStream(zipCom("diario.json", json.toByteArray())))
        val jsonAvulso = BufferedInputStream(ByteArrayInputStream(json.toByteArray()))

        assertTrue(PacoteDeBackup.eZip(zip))
        assertFalse(PacoteDeBackup.eZip(jsonAvulso))
        // Depois da checagem, a leitura continua do primeiro byte.
        assertEquals(json, String(jsonAvulso.readBytes(), Charsets.UTF_8))
    }

    private fun zipCom(nome: String, conteudo: ByteArray): ByteArray {
        val saida = ByteArrayOutputStream()
        ZipOutputStream(saida).use { zip ->
            zip.putNextEntry(ZipEntry(nome))
            zip.write(conteudo)
            zip.closeEntry()
        }
        return saida.toByteArray()
    }
}
