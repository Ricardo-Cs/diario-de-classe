package br.com.ricardo.diariodeclasse.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import br.com.ricardo.diariodeclasse.data.local.AppDatabase
import br.com.ricardo.diariodeclasse.data.local.dao.AlunoDao
import br.com.ricardo.diariodeclasse.data.local.dao.AnotacaoDao
import br.com.ricardo.diariodeclasse.data.local.dao.BackupDao
import br.com.ricardo.diariodeclasse.data.local.dao.ChamadaDao
import br.com.ricardo.diariodeclasse.data.local.dao.FotoDao
import br.com.ricardo.diariodeclasse.data.local.dao.LembreteDao
import br.com.ricardo.diariodeclasse.data.local.dao.MetaDao
import br.com.ricardo.diariodeclasse.data.local.dao.MetricaDao
import br.com.ricardo.diariodeclasse.data.local.dao.PendenciaDao
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

    /**
     * Por padrão o Room usa o modo WAL: as gravações recentes ficam num arquivo
     * à parte (`diario.db-wal`) até serem juntadas ao banco. O Auto Backup do
     * Android copia os arquivos e poderia levar um `diario.db` sem as últimas
     * alterações. No modo `TRUNCATE`, cada gravação vai direto para o `diario.db`,
     * então o arquivo copiado está sempre completo. Para um app deste tamanho,
     * a diferença de desempenho não é perceptível.
     */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NOME)
            .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
            .build()
    }

    @Provides
    fun provideTurmaDao(database: AppDatabase): TurmaDao = database.turmaDao()

    @Provides
    fun provideAlunoDao(database: AppDatabase): AlunoDao = database.alunoDao()

    @Provides
    fun provideChamadaDao(database: AppDatabase): ChamadaDao = database.chamadaDao()

    @Provides
    fun providePendenciaDao(database: AppDatabase): PendenciaDao = database.pendenciaDao()

    @Provides
    fun provideAnotacaoDao(database: AppDatabase): AnotacaoDao = database.anotacaoDao()

    @Provides
    fun provideMetricaDao(database: AppDatabase): MetricaDao = database.metricaDao()

    @Provides
    fun provideMetaDao(database: AppDatabase): MetaDao = database.metaDao()

    @Provides
    fun provideLembreteDao(database: AppDatabase): LembreteDao = database.lembreteDao()

    @Provides
    fun provideFotoDao(database: AppDatabase): FotoDao = database.fotoDao()

    @Provides
    fun provideBackupDao(database: AppDatabase): BackupDao = database.backupDao()

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemDefaultZone()
}
