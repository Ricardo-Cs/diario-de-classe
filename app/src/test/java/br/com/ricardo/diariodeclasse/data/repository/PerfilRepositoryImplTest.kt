package br.com.ricardo.diariodeclasse.data.repository

import android.net.Uri
import br.com.ricardo.diariodeclasse.data.fotos.ArquivoDaCamera
import br.com.ricardo.diariodeclasse.data.fotos.ArquivosDeFotos
import br.com.ricardo.diariodeclasse.data.local.dao.PerfilDao
import br.com.ricardo.diariodeclasse.data.local.entity.Perfil
import br.com.ricardo.diariodeclasse.data.local.entity.ResumoDoPerfil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/** DAO em memória com, no máximo, um perfil. */
private class FakePerfilDao : PerfilDao {
    val linha = MutableStateFlow<Perfil?>(null)

    override fun observar(): Flow<Perfil?> = linha

    override suspend fun buscar(): Perfil? = linha.value

    override fun observarResumo(): Flow<ResumoDoPerfil> {
        return linha.map { _ -> ResumoDoPerfil(turmas = 0, alunos = 0, anotacoes = 0) }
    }

    override suspend fun inserir(perfil: Perfil) {
        linha.value = perfil
    }

    override suspend fun atualizar(perfil: Perfil) {
        linha.value = perfil
    }
}

/** Arquivos numa pasta temporária. Reduzir imagens depende do Android e não é testado aqui. */
private class FakeArquivosDoPerfil(private val pasta: File) : ArquivosDeFotos {
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

class PerfilRepositoryImplTest {
    @get:Rule
    val pastas = TemporaryFolder()

    private val inicio = Instant.parse("2026-10-09T11:00:00Z")
    private val dao = FakePerfilDao()

    private fun repositorioNoInstante(agora: Instant): PerfilRepositoryImpl {
        return PerfilRepositoryImpl(dao, FakeArquivosDoPerfil(pastas.root), Clock.fixed(agora, ZoneOffset.UTC))
    }

    @Test
    fun salvarNomeEEscola_semPerfilCriaUm() = runBlocking {
        repositorioNoInstante(inicio).salvarNomeEEscola("  Maria Souza ", " EMEF Monteiro Lobato ")

        val salvo: Perfil = dao.buscar()!!
        assertEquals("Maria Souza", salvo.nome)
        assertEquals("EMEF Monteiro Lobato", salvo.escola)
        assertEquals(inicio, salvo.createdAt)
    }

    @Test
    fun salvarNomeEEscola_comPerfilAtualizaOMesmo() = runBlocking {
        repositorioNoInstante(inicio).salvarNomeEEscola("Maria", "")
        val idOriginal: String = dao.buscar()!!.id
        val umaHoraDepois = inicio.plusSeconds(3600)

        repositorioNoInstante(umaHoraDepois).salvarNomeEEscola("Maria Souza", "")

        val salvo: Perfil = dao.buscar()!!
        assertEquals(idOriginal, salvo.id)
        assertEquals("Maria Souza", salvo.nome)
        assertEquals(inicio, salvo.createdAt)
        assertEquals(umaHoraDepois, salvo.updatedAt)
    }

    @Test
    fun salvarNomeEEscola_escolaVaziaFicaSemEscola() = runBlocking {
        repositorioNoInstante(inicio).salvarNomeEEscola("Maria", "   ")

        assertNull(dao.buscar()!!.escola)
    }

    @Test
    fun removerFoto_tiraDoPerfilEApagaOArquivo() = runBlocking {
        val imagem: File = pastas.newFile("perfil-antiga.jpg")
        dao.inserir(
            Perfil(
                id = "perfil", nome = "Maria", nomeDoArquivoDaFoto = "perfil-antiga.jpg",
                createdAt = inicio, updatedAt = inicio,
            ),
        )

        repositorioNoInstante(inicio).removerFoto()

        assertNull(dao.buscar()!!.nomeDoArquivoDaFoto)
        assertFalse(imagem.exists())
    }

    @Test
    fun arquivoDaFoto_semFotoENulo() {
        val semFoto = Perfil(id = "perfil", nome = "Maria", createdAt = inicio, updatedAt = inicio)

        assertNull(repositorioNoInstante(inicio).arquivoDaFoto(semFoto))
    }
}
