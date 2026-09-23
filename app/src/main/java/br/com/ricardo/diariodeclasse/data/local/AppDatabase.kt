package br.com.ricardo.diariodeclasse.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import br.com.ricardo.diariodeclasse.data.local.dao.AlunoDao
import br.com.ricardo.diariodeclasse.data.local.dao.TurmaDao
import br.com.ricardo.diariodeclasse.data.local.entity.Aluno
import br.com.ricardo.diariodeclasse.data.local.entity.Turma

/**
 * Ao adicionar ou alterar entidades, incremente [version] e declare a migração:
 * o app substitui o caderno, então nunca usar `fallbackToDestructiveMigration`.
 *
 * Histórico:
 * 1 → turmas
 * 2 → alunos
 */
@Database(
    entities = [Turma::class, Aluno::class],
    version = 2,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
    ],
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun turmaDao(): TurmaDao
    abstract fun alunoDao(): AlunoDao

    companion object {
        const val NOME = "diario.db"
    }
}
