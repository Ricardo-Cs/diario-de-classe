package br.com.ricardo.diariodeclasse.data.repository

import android.net.Uri
import br.com.ricardo.diariodeclasse.data.fotos.ArquivoDaCamera
import br.com.ricardo.diariodeclasse.data.fotos.ArquivosDeFotos
import br.com.ricardo.diariodeclasse.data.local.dao.FotoDao
import br.com.ricardo.diariodeclasse.data.local.entity.Foto
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

/** Fotos do registro do dia de cada turma: o banco guarda os dados, [ArquivosDeFotos] as imagens. */
interface FotoRepository {
    fun observarDaTurma(turmaId: String): Flow<List<Foto>>

    /** Onde está a imagem da foto (o arquivo pode não existir, ver `FotoScreen`). */
    fun arquivoDa(foto: Foto): File

    /** Reduz a imagem de [origem] (ex.: escolhida na galeria) e cria a foto. Lança `IOException`. */
    suspend fun adicionar(turmaId: String, data: LocalDate, origem: Uri): Foto

    fun prepararCamera(): ArquivoDaCamera

    /** Cria a foto a partir do arquivo que a câmera gravou e apaga o arquivo temporário. */
    suspend fun adicionarDaCamera(turmaId: String, data: LocalDate, nomeTemporario: String): Foto

    /** A professora fechou a câmera sem tirar a foto. */
    suspend fun descartarCamera(nomeTemporario: String)

    /** Legenda vazia vira `null` (sem legenda). */
    suspend fun editarLegenda(fotoId: String, legenda: String)

    /**
     * Soft delete, como o resto do app: o arquivo continua no aparelho, mas
     * fica fora da exportação.
     */
    suspend fun excluir(fotoId: String)
}

class FotoRepositoryImpl @Inject constructor(
    private val dao: FotoDao,
    private val arquivos: ArquivosDeFotos,
    private val clock: Clock,
) : FotoRepository {

    override fun observarDaTurma(turmaId: String): Flow<List<Foto>> {
        return dao.observarDaTurma(turmaId)
    }

    override fun arquivoDa(foto: Foto): File {
        return arquivos.arquivoDa(foto.nomeDoArquivo)
    }

    /**
     * O arquivo é gravado antes da linha no banco: se a imagem não puder ser
     * lida, nenhuma foto "vazia" aparece na tela.
     */
    override suspend fun adicionar(turmaId: String, data: LocalDate, origem: Uri): Foto {
        val id: String = UUID.randomUUID().toString()
        val nomeDoArquivo = "$id.jpg"
        arquivos.salvarReduzida(origem, nomeDoArquivo)

        val agora: Instant = Instant.now(clock)
        val foto = Foto(
            id = id,
            turmaId = turmaId,
            data = data,
            nomeDoArquivo = nomeDoArquivo,
            createdAt = agora,
            updatedAt = agora,
        )
        dao.inserir(foto)
        return foto
    }

    override fun prepararCamera(): ArquivoDaCamera {
        return arquivos.criarArquivoDaCamera()
    }

    override suspend fun adicionarDaCamera(turmaId: String, data: LocalDate, nomeTemporario: String): Foto {
        val arquivoDaCamera: File = arquivos.arquivoDaCamera(nomeTemporario)
        if (!arquivoDaCamera.exists() || arquivoDaCamera.length() == 0L) {
            throw IOException("A câmera não gravou a foto")
        }
        try {
            return adicionar(turmaId, data, Uri.fromFile(arquivoDaCamera))
        } finally {
            // `finally` roda com sucesso ou erro: o temporário nunca fica para trás.
            arquivoDaCamera.delete()
        }
    }

    override suspend fun descartarCamera(nomeTemporario: String) {
        arquivos.arquivoDaCamera(nomeTemporario).delete()
    }

    override suspend fun editarLegenda(fotoId: String, legenda: String) {
        val foto: Foto = dao.buscarPorId(fotoId) ?: return
        val legendaLimpa: String = legenda.trim()
        var novaLegenda: String? = null
        if (legendaLimpa.isNotEmpty()) {
            novaLegenda = legendaLimpa
        }
        dao.atualizar(foto.copy(legenda = novaLegenda, updatedAt = Instant.now(clock)))
    }

    override suspend fun excluir(fotoId: String) {
        dao.marcarComoExcluida(fotoId, Instant.now(clock))
    }
}
