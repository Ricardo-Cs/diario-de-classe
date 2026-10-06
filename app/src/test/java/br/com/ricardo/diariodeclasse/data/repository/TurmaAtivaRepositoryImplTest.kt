package br.com.ricardo.diariodeclasse.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import br.com.ricardo.diariodeclasse.data.local.entity.Periodo
import br.com.ricardo.diariodeclasse.data.local.entity.Turma
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.Instant

/** Só [observarTurmas] importa para estes testes. */
private class FakeTurmaRepository : TurmaRepository {
    val turmas = MutableStateFlow<List<Turma>>(emptyList())

    override fun observarTurmas(): Flow<List<Turma>> = turmas
    override fun observarTurma(id: String): Flow<Turma?> = throw NotImplementedError()
    override suspend fun buscarTurma(id: String): Turma? = throw NotImplementedError()
    override suspend fun criar(nome: String, anoSerie: String, periodo: Periodo, anoLetivo: Int): Turma =
        throw NotImplementedError()
    override suspend fun atualizar(turma: Turma) = throw NotImplementedError()
    override suspend fun excluir(id: String) = throw NotImplementedError()
}

private fun turmaDeTeste(id: String): Turma {
    val agora = Instant.parse("2026-02-02T11:00:00Z")
    return Turma(
        id = id,
        nome = "Turma $id",
        anoSerie = "1º ano",
        periodo = Periodo.MANHA,
        anoLetivo = 2026,
        createdAt = agora,
        updatedAt = agora,
    )
}

/** Usa um DataStore de verdade, gravando num arquivo temporário apagado ao fim de cada teste. */
class TurmaAtivaRepositoryImplTest {

    @get:Rule
    val pastaTemporaria = TemporaryFolder()

    private val escopoDoDataStore = CoroutineScope(Dispatchers.IO + Job())
    private val turmaRepository = FakeTurmaRepository()

    private val turmaA = turmaDeTeste("a")
    private val turmaB = turmaDeTeste("b")

    private fun criarRepositorio(): TurmaAtivaRepositoryImpl {
        val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
            scope = escopoDoDataStore,
            produceFile = { File(pastaTemporaria.root, "teste.preferences_pb") },
        )
        return TurmaAtivaRepositoryImpl(dataStore, turmaRepository)
    }

    @After
    fun fecharDataStore() {
        escopoDoDataStore.cancel()
    }

    @Test
    fun semTurmas_emiteNull() = runBlocking {
        val repositorio = criarRepositorio()

        assertNull(repositorio.observarTurmaAtiva().first())
    }

    @Test
    fun nadaSelecionado_usaAPrimeiraTurma() = runBlocking {
        turmaRepository.turmas.value = listOf(turmaA, turmaB)
        val repositorio = criarRepositorio()

        assertEquals(turmaA, repositorio.observarTurmaAtiva().first())
    }

    @Test
    fun selecionar_passaAEmitirATurmaEscolhida() = runBlocking {
        turmaRepository.turmas.value = listOf(turmaA, turmaB)
        val repositorio = criarRepositorio()

        repositorio.selecionar(turmaB.id)

        assertEquals(turmaB, repositorio.observarTurmaAtiva().first())
    }

    @Test
    fun turmaSelecionadaExcluida_voltaParaAPrimeira() = runBlocking {
        turmaRepository.turmas.value = listOf(turmaA, turmaB)
        val repositorio = criarRepositorio()
        repositorio.selecionar(turmaB.id)

        turmaRepository.turmas.value = listOf(turmaA)

        assertEquals(turmaA, repositorio.observarTurmaAtiva().first())
    }
}
