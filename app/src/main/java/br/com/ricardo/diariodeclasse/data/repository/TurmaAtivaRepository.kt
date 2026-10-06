package br.com.ricardo.diariodeclasse.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import br.com.ricardo.diariodeclasse.data.local.entity.Turma
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Turma em que a professora está trabalhando no momento. Quase todas as telas
 * (Início, chamada, pendências) partem dela.
 */
interface TurmaAtivaRepository {
    /** Emite `null` somente quando não há nenhuma turma cadastrada. */
    fun observarTurmaAtiva(): Flow<Turma?>
    suspend fun selecionar(turmaId: String)
}

/**
 * A escolha fica salva no DataStore, um arquivo de chave/valor do Android
 * (parecido com o `localStorage`, mas assíncrono e observável como Flow).
 */
class TurmaAtivaRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val turmaRepository: TurmaRepository,
) : TurmaAtivaRepository {

    override fun observarTurmaAtiva(): Flow<Turma?> {
        val idSalvo: Flow<String?> = dataStore.data.map { preferencias ->
            preferencias[CHAVE_TURMA_ATIVA]
        }
        return combine(turmaRepository.observarTurmas(), idSalvo) { turmas, turmaId ->
            escolherTurmaAtiva(turmas, turmaId)
        }
    }

    override suspend fun selecionar(turmaId: String) {
        dataStore.edit { preferencias ->
            preferencias[CHAVE_TURMA_ATIVA] = turmaId
        }
    }

    /**
     * Se a turma salva foi excluída (ou nada foi escolhido ainda),
     * usa a primeira da lista em vez de deixar a tela sem turma.
     */
    private fun escolherTurmaAtiva(turmas: List<Turma>, turmaIdSalvo: String?): Turma? {
        if (turmas.isEmpty()) {
            return null
        }
        for (turma in turmas) {
            if (turma.id == turmaIdSalvo) {
                return turma
            }
        }
        return turmas.first()
    }

    /** `companion object` guarda membros "estáticos" da classe, como o `static` do Java. */
    companion object {
        private val CHAVE_TURMA_ATIVA = stringPreferencesKey("turma_ativa_id")
    }
}
