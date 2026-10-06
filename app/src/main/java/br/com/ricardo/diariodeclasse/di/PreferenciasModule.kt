package br.com.ricardo.diariodeclasse.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * O DataStore precisa ser único no app: duas instâncias abrindo o mesmo arquivo
 * geram erro. Por isso `@Singleton`.
 */
@Module
@InstallIn(SingletonComponent::class)
object PreferenciasModule {

    private const val NOME_DO_ARQUIVO = "preferencias"

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile(NOME_DO_ARQUIVO) },
        )
    }
}
