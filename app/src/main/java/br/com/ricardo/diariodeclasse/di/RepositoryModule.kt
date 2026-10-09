package br.com.ricardo.diariodeclasse.di

import br.com.ricardo.diariodeclasse.data.fotos.ArquivosDeFotos
import br.com.ricardo.diariodeclasse.data.fotos.ArquivosDeFotosImpl
import br.com.ricardo.diariodeclasse.data.repository.AlunoRepository
import br.com.ricardo.diariodeclasse.data.repository.AlunoRepositoryImpl
import br.com.ricardo.diariodeclasse.data.repository.AnotacaoRepository
import br.com.ricardo.diariodeclasse.data.repository.AnotacaoRepositoryImpl
import br.com.ricardo.diariodeclasse.data.repository.BackupRepository
import br.com.ricardo.diariodeclasse.data.repository.BackupRepositoryImpl
import br.com.ricardo.diariodeclasse.data.repository.ChamadaRepository
import br.com.ricardo.diariodeclasse.data.repository.ChamadaRepositoryImpl
import br.com.ricardo.diariodeclasse.data.repository.FotoRepository
import br.com.ricardo.diariodeclasse.data.repository.FotoRepositoryImpl
import br.com.ricardo.diariodeclasse.data.repository.LembreteDiarioRepository
import br.com.ricardo.diariodeclasse.data.repository.LembreteDiarioRepositoryImpl
import br.com.ricardo.diariodeclasse.data.repository.LembreteRepository
import br.com.ricardo.diariodeclasse.data.repository.LembreteRepositoryImpl
import br.com.ricardo.diariodeclasse.data.repository.MetaRepository
import br.com.ricardo.diariodeclasse.data.repository.MetaRepositoryImpl
import br.com.ricardo.diariodeclasse.data.repository.MetricaRepository
import br.com.ricardo.diariodeclasse.data.repository.MetricaRepositoryImpl
import br.com.ricardo.diariodeclasse.data.repository.PendenciaRepository
import br.com.ricardo.diariodeclasse.data.repository.PendenciaRepositoryImpl
import br.com.ricardo.diariodeclasse.data.repository.TurmaAtivaRepository
import br.com.ricardo.diariodeclasse.data.repository.TurmaAtivaRepositoryImpl
import br.com.ricardo.diariodeclasse.data.repository.TurmaRepository
import br.com.ricardo.diariodeclasse.data.repository.TurmaRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** `@Binds` liga a interface à implementação (quem pede [TurmaRepository] recebe o Impl). */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTurmaRepository(impl: TurmaRepositoryImpl): TurmaRepository

    @Binds
    @Singleton
    abstract fun bindAlunoRepository(impl: AlunoRepositoryImpl): AlunoRepository

    @Binds
    @Singleton
    abstract fun bindTurmaAtivaRepository(impl: TurmaAtivaRepositoryImpl): TurmaAtivaRepository

    @Binds
    @Singleton
    abstract fun bindChamadaRepository(impl: ChamadaRepositoryImpl): ChamadaRepository

    @Binds
    @Singleton
    abstract fun bindPendenciaRepository(impl: PendenciaRepositoryImpl): PendenciaRepository

    @Binds
    @Singleton
    abstract fun bindAnotacaoRepository(impl: AnotacaoRepositoryImpl): AnotacaoRepository

    @Binds
    @Singleton
    abstract fun bindBackupRepository(impl: BackupRepositoryImpl): BackupRepository

    @Binds
    @Singleton
    abstract fun bindLembreteDiarioRepository(impl: LembreteDiarioRepositoryImpl): LembreteDiarioRepository

    @Binds
    @Singleton
    abstract fun bindMetricaRepository(impl: MetricaRepositoryImpl): MetricaRepository

    @Binds
    @Singleton
    abstract fun bindMetaRepository(impl: MetaRepositoryImpl): MetaRepository

    @Binds
    @Singleton
    abstract fun bindLembreteRepository(impl: LembreteRepositoryImpl): LembreteRepository

    @Binds
    @Singleton
    abstract fun bindFotoRepository(impl: FotoRepositoryImpl): FotoRepository

    @Binds
    @Singleton
    abstract fun bindArquivosDeFotos(impl: ArquivosDeFotosImpl): ArquivosDeFotos
}
