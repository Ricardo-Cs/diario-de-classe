package br.com.ricardo.diariodeclasse.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

/**
 * Lembra em que dia a notificação de pendências já foi mostrada, para
 * avisar uma vez só por dia mesmo com a verificação rodando de hora em hora.
 */
interface LembreteDiarioRepository {
    /** `null` quando nunca avisou. */
    suspend fun buscarUltimoDiaAvisado(): LocalDate?
    suspend fun registrarDiaAvisado(data: LocalDate)
}

class LembreteDiarioRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : LembreteDiarioRepository {

    /** `first()` lê o valor atual do Flow uma vez, em vez de ficar observando. */
    override suspend fun buscarUltimoDiaAvisado(): LocalDate? {
        val preferencias: Preferences = dataStore.data.first()
        val dataSalva: String? = preferencias[CHAVE_ULTIMO_DIA_AVISADO]
        if (dataSalva == null) {
            return null
        }
        return LocalDate.parse(dataSalva)
    }

    override suspend fun registrarDiaAvisado(data: LocalDate) {
        dataStore.edit { preferencias ->
            preferencias[CHAVE_ULTIMO_DIA_AVISADO] = data.toString()
        }
    }

    companion object {
        private val CHAVE_ULTIMO_DIA_AVISADO = stringPreferencesKey("lembrete_ultimo_dia_avisado")
    }
}
