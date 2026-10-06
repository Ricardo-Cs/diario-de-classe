package br.com.ricardo.diariodeclasse.di

import br.com.ricardo.diariodeclasse.data.repository.AlunoRepository
import br.com.ricardo.diariodeclasse.data.repository.AlunoRepositoryImpl
import br.com.ricardo.diariodeclasse.data.repository.AnotacaoRepository
import br.com.ricardo.diariodeclasse.data.repository.AnotacaoRepositoryImpl
import br.com.ricardo.diariodeclasse.data.repository.AtividadeRecenteRepository
import br.com.ricardo.diariodeclasse.data.repository.AtividadeRecenteRepositoryImpl
import br.com.ricardo.diariodeclasse.data.repository.BackupRepository
import br.com.ricardo.diariodeclasse.data.repository.BackupRepositoryImpl
import br.com.ricardo.diariodeclasse.data.repository.ChamadaRepository
import br.com.ricardo.diariodeclasse.data.repository.ChamadaRepositoryImpl
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
    abstract fun bindAtividadeRecenteRepository(impl: AtividadeRecenteRepositoryImpl): AtividadeRecenteRepository

    @Binds
    @Singleton
    abstract fun bindBackupRepository(impl: BackupRepositoryImpl): BackupRepository
}
