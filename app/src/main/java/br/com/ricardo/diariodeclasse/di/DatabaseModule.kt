package br.com.ricardo.diariodeclasse.di

import android.content.Context
import androidx.room.Room
import br.com.ricardo.diariodeclasse.data.local.AppDatabase
import br.com.ricardo.diariodeclasse.data.local.dao.AlunoDao
import br.com.ricardo.diariodeclasse.data.local.dao.TurmaDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

/**
 * `@Provides` ensina o Hilt a construir tipos que não são nossos (Room, Clock),
 * como um `@Bean` do Spring. `SingletonComponent` = vive enquanto o app viver.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NOME).build()

    @Provides
    fun provideTurmaDao(database: AppDatabase): TurmaDao = database.turmaDao()

    @Provides
    fun provideAlunoDao(database: AppDatabase): AlunoDao = database.alunoDao()

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemDefaultZone()
}
