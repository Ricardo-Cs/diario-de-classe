package br.com.ricardo.diariodeclasse.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import br.com.ricardo.diariodeclasse.data.local.dao.TurmaDao
import br.com.ricardo.diariodeclasse.data.local.entity.Turma

/**
 * Ao adicionar ou alterar entidades, incremente [version] e escreva a migração:
 * o app substitui o caderno, então nunca usar `fallbackToDestructiveMigration`.
 */
@Database(
    entities = [Turma::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun turmaDao(): TurmaDao

    companion object {
        const val NOME = "diario.db"
    }
}
