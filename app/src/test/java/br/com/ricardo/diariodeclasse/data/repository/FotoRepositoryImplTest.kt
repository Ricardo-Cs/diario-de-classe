package br.com.ricardo.diariodeclasse.data.repository

import android.net.Uri
import br.com.ricardo.diariodeclasse.data.fotos.ArquivoDaCamera
import br.com.ricardo.diariodeclasse.data.fotos.ArquivosDeFotos
import br.com.ricardo.diariodeclasse.data.local.dao.FotoDao
import br.com.ricardo.diariodeclasse.data.local.entity.Foto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** DAO em memória que imita o filtro de soft delete. */
private class FakeFotoDao : FotoDao {
    val linhas = MutableStateFlow<List<Foto>>(emptyList())

    override fun observarDaTurma(turmaId: String): Flow<List<Foto>> {
        return linhas.map { fotos ->
            fotos.filter { foto -> foto.turmaId == turmaId && foto.deletedAt == null }
        }
    }

    override suspend fun buscarPorId(id: String): Foto? {
        return linhas.value.firstOrNull { foto -> foto.id == id }
    }

    override suspend fun inserir(foto: Foto) {
        linhas.value = linhas.value + foto
    }

    override suspend fun atualizar(foto: Foto) {
        linhas.value = linhas.value.map { existente ->
            if (existente.id == foto.id) foto else existente
        }
    }

    override suspend fun marcarComoExcluida(id: String, agora: Instant) {
        val excluida = buscarPorId(id)!!.copy(deletedAt = agora, updatedAt = agora)
        atualizar(excluida)
    }
}

/** Arquivos numa pasta temporária. Reduzir imagens depende do Android e não é testado aqui. */
private class FakeArquivosDeFotos(private val pasta: File) : ArquivosDeFotos {
    override fun arquivoDa(nomeDoArquivo: String): File = File(pasta, nomeDoArquivo)
    override suspend fun salvarReduzida(origem: Uri, nomeDoArquivo: String) = error("não usado")
    override suspend fun apagar(nomeDoArquivo: String) {
        arquivoDa(nomeDoArquivo).delete()
    }
    override fun criarArquivoDaCamera(): ArquivoDaCamera = error("não usado")
    override fun arquivoDaCamera(nomeTemporario: String): File = File(pasta, nomeTemporario)
    override suspend fun prepararPastaDeImportacao(): File = error("não usado")
    override suspend fun substituirPelaImportacao(pastaImportada: File) = error("não usado")
    override suspend fun descartarImportacao(pastaImportada: File) = error("não usado")
}

class FotoRepositoryImplTest {
    @get:Rule
    val pastas = TemporaryFolder()

    private val inicio = Instant.parse("2026-10-09T11:00:00Z")
    private val hoje = LocalDate.of(2026, 10, 9)
    private val dao = FakeFotoDao()

    private fun repositorioNoInstante(agora: Instant): FotoRepositoryImpl {
        return FotoRepositoryImpl(dao, FakeArquivosDeFotos(pastas.root), Clock.fixed(agora, ZoneOffset.UTC))
    }

    private fun fotoSalva(): Foto {
        val foto = Foto(
            id = "foto", turmaId = "turma", data = hoje, nomeDoArquivo = "foto.jpg",
            createdAt = inicio, updatedAt = inicio,
        )
        runBlocking { dao.inserir(foto) }
        return foto
    }

    @Test
    fun editarLegenda_tiraEspacosEAtualizaUpdatedAt() = runBlocking {
        fotoSalva()
        val umaHoraDepois = inicio.plusSeconds(3600)

        repositorioNoInstante(umaHoraDepois).editarLegenda("foto", "  Pintura com guache ")

        val salva = dao.buscarPorId("foto")!!
        assertEquals("Pintura com guache", salva.legenda)
        assertEquals(inicio, salva.createdAt)
        assertEquals(umaHoraDepois, salva.updatedAt)
    }

    @Test
    fun editarLegenda_vaziaTiraALegenda() = runBlocking {
        val foto: Foto = fotoSalva()
        dao.atualizar(foto.copy(legenda = "Antiga"))

        repositorioNoInstante(inicio).editarLegenda("foto", "   ")

        assertNull(dao.buscarPorId("foto")!!.legenda)
    }

    @Test
    fun excluir_someDaTurmaMasContinuaNoBanco() = runBlocking {
        fotoSalva()

        repositorioNoInstante(inicio).excluir("foto")

        assertTrue(repositorioNoInstante(inicio).observarDaTurma("turma").first().isEmpty())
        assertEquals(inicio, dao.buscarPorId("foto")!!.deletedAt)
    }

    @Test
    fun adicionarDaCamera_semArquivoGravado_falhaSemCriarFoto() = runBlocking {
        var falhou = false
        try {
            repositorioNoInstante(inicio).adicionarDaCamera("turma", hoje, "camera-123.jpg")
        } catch (erro: IOException) {
            falhou = true
        }

        assertTrue(falhou)
        assertTrue(dao.linhas.value.isEmpty())
    }

    @Test
    fun descartarCamera_apagaOArquivoTemporario() = runBlocking {
        val temporario: File = pastas.newFile("camera-123.jpg")

        repositorioNoInstante(inicio).descartarCamera("camera-123.jpg")

        assertFalse(temporario.exists())
    }
}
