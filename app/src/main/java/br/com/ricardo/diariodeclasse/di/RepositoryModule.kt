package br.com.ricardo.diariodeclasse.di

import br.com.ricardo.diariodeclasse.data.repository.AlunoRepository
import br.com.ricardo.diariodeclasse.data.repository.AlunoRepositoryImpl
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
}
