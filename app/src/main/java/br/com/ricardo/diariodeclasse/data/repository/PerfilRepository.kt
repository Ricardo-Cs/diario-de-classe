package br.com.ricardo.diariodeclasse.data.repository

import android.net.Uri
import br.com.ricardo.diariodeclasse.data.fotos.ArquivoDaCamera
import br.com.ricardo.diariodeclasse.data.fotos.ArquivosDeFotos
import br.com.ricardo.diariodeclasse.data.local.dao.PerfilDao
import br.com.ricardo.diariodeclasse.data.local.entity.Perfil
import br.com.ricardo.diariodeclasse.data.local.entity.ResumoDoPerfil
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

/**
 * O perfil da professora. A foto usa a mesma pasta e a mesma redução das fotos
 * do dia ([ArquivosDeFotos]), então vai junto na exportação.
 */
interface PerfilRepository {
    /** `null` enquanto ela não salvou nada no perfil. */
    fun observarPerfil(): Flow<Perfil?>

    fun observarResumo(): Flow<ResumoDoPerfil>

    /** Onde está a foto do perfil; `null` se ela não tiver foto. */
    fun arquivoDaFoto(perfil: Perfil): File?

    /** Nome e escola sem espaços nas pontas; escola vazia vira `null`. */
    suspend fun salvarNomeEEscola(nome: String, escola: String)

    /** Reduz a imagem de [origem] (ex.: escolhida na galeria) e troca a foto. Lança `IOException`. */
    suspend fun trocarFoto(origem: Uri)

    fun prepararCamera(): ArquivoDaCamera

    /** Troca a foto pela que a câmera gravou e apaga o arquivo temporário. */
    suspend fun trocarFotoPelaCamera(nomeTemporario: String)

    /** A professora fechou a câmera sem tirar a foto. */
    suspend fun descartarCamera(nomeTemporario: String)

    suspend fun removerFoto()
}

class PerfilRepositoryImpl @Inject constructor(
    private val dao: PerfilDao,
    private val arquivos: ArquivosDeFotos,
    private val clock: Clock,
) : PerfilRepository {

    override fun observarPerfil(): Flow<Perfil?> {
        return dao.observar()
    }

    override fun observarResumo(): Flow<ResumoDoPerfil> {
        return dao.observarResumo()
    }

    override fun arquivoDaFoto(perfil: Perfil): File? {
        val nomeDaFoto: String? = perfil.nomeDoArquivoDaFoto
        if (nomeDaFoto == null) {
            return null
        }
        return arquivos.arquivoDa(nomeDaFoto)
    }

    override suspend fun salvarNomeEEscola(nome: String, escola: String) {
        val escolaLimpa: String = escola.trim()
        var novaEscola: String? = null
        if (escolaLimpa.isNotEmpty()) {
            novaEscola = escolaLimpa
        }
        val perfil: Perfil = buscarOuCriar()
        dao.atualizar(perfil.copy(nome = nome.trim(), escola = novaEscola, updatedAt = Instant.now(clock)))
    }

    /**
     * A imagem é gravada antes de o perfil apontar para ela: se a leitura falhar,
     * a foto antiga continua. Só depois o arquivo antigo é apagado, porque
     * nenhuma outra linha usa a foto do perfil.
     */
    override suspend fun trocarFoto(origem: Uri) {
        val nomeDoArquivo = "perfil-${UUID.randomUUID()}.jpg"
        arquivos.salvarReduzida(origem, nomeDoArquivo)

        val perfil: Perfil = buscarOuCriar()
        dao.atualizar(perfil.copy(nomeDoArquivoDaFoto = nomeDoArquivo, updatedAt = Instant.now(clock)))
        apagarArquivoDaFoto(perfil.nomeDoArquivoDaFoto)
    }

    override fun prepararCamera(): ArquivoDaCamera {
        return arquivos.criarArquivoDaCamera()
    }

    override suspend fun trocarFotoPelaCamera(nomeTemporario: String) {
        val arquivoDaCamera: File = arquivos.arquivoDaCamera(nomeTemporario)
        if (!arquivoDaCamera.exists() || arquivoDaCamera.length() == 0L) {
            throw IOException("A câmera não gravou a foto")
        }
        try {
            trocarFoto(Uri.fromFile(arquivoDaCamera))
        } finally {
            // `finally` roda com sucesso ou erro: o temporário nunca fica para trás.
            arquivoDaCamera.delete()
        }
    }

    override suspend fun descartarCamera(nomeTemporario: String) {
        arquivos.arquivoDaCamera(nomeTemporario).delete()
    }

    override suspend fun removerFoto() {
        val perfil: Perfil = dao.buscar() ?: return
        dao.atualizar(perfil.copy(nomeDoArquivoDaFoto = null, updatedAt = Instant.now(clock)))
        apagarArquivoDaFoto(perfil.nomeDoArquivoDaFoto)
    }

    /** Ela pode começar pela foto, antes de ter um nome: o perfil nasce vazio. */
    private suspend fun buscarOuCriar(): Perfil {
        val existente: Perfil? = dao.buscar()
        if (existente != null) {
            return existente
        }
        val agora: Instant = Instant.now(clock)
        val novo = Perfil(
            id = UUID.randomUUID().toString(),
            nome = "",
            createdAt = agora,
            updatedAt = agora,
        )
        dao.inserir(novo)
        return novo
    }

    private suspend fun apagarArquivoDaFoto(nomeDoArquivo: String?) {
        if (nomeDoArquivo != null) {
            arquivos.apagar(nomeDoArquivo)
        }
    }
}
