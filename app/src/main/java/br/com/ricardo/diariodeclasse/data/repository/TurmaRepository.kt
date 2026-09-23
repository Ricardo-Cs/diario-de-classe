package br.com.ricardo.diariodeclasse.data.repository

import br.com.ricardo.diariodeclasse.data.local.dao.TurmaDao
import br.com.ricardo.diariodeclasse.data.local.entity.Periodo
import br.com.ricardo.diariodeclasse.data.local.entity.Turma
import kotlinx.coroutines.flow.Flow
import java.time.Clock
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

/**
 * Única porta de acesso às turmas para os ViewModels. Quando houver backend,
 * a sincronização entra aqui sem mudar nada na UI.
 */
interface TurmaRepository {
    fun observarTurmas(): Flow<List<Turma>>
    fun observarTurma(id: String): Flow<Turma?>
    suspend fun criar(nome: String, anoSerie: String, periodo: Periodo, anoLetivo: Int): Turma
    suspend fun atualizar(turma: Turma)
    suspend fun excluir(id: String)
}

/** O [Clock] é injetado para que os testes controlem o "agora". */
class TurmaRepositoryImpl @Inject constructor(
    private val dao: TurmaDao,
    private val clock: Clock,
) : TurmaRepository {

    override fun observarTurmas(): Flow<List<Turma>> = dao.observarTodas()

    override fun observarTurma(id: String): Flow<Turma?> = dao.observarPorId(id)

    override suspend fun criar(
        nome: String,
        anoSerie: String,
        periodo: Periodo,
        anoLetivo: Int,
    ): Turma {
        val agora = Instant.now(clock)
        val turma = Turma(
            id = UUID.randomUUID().toString(),
            nome = nome,
            anoSerie = anoSerie,
            periodo = periodo,
            anoLetivo = anoLetivo,
            createdAt = agora,
            updatedAt = agora,
        )
        dao.inserir(turma)
        return turma
    }

    override suspend fun atualizar(turma: Turma) {
        dao.atualizar(turma.copy(updatedAt = Instant.now(clock)))
    }

    override suspend fun excluir(id: String) {
        dao.marcarComoExcluida(id, Instant.now(clock))
    }
}
