package br.com.ricardo.diariodeclasse.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import br.com.ricardo.diariodeclasse.data.local.dao.AlunoDao
import br.com.ricardo.diariodeclasse.data.local.dao.ChamadaDao
import br.com.ricardo.diariodeclasse.data.local.dao.TurmaDao
import br.com.ricardo.diariodeclasse.data.local.entity.Aluno
import br.com.ricardo.diariodeclasse.data.local.entity.Chamada
import br.com.ricardo.diariodeclasse.data.local.entity.RegistroPresenca
import br.com.ricardo.diariodeclasse.data.local.entity.Turma

/**
 * Ao adicionar ou alterar entidades, incremente [version] e declare a migração:
 * o app substitui o caderno, então nunca usar `fallbackToDestructiveMigration`.
 *
 * Histórico:
 * 1 → turmas
 * 2 → alunos
 * 3 → chamadas e registros de presença
 */
@Database(
    entities = [Turma::class, Aluno::class, Chamada::class, RegistroPresenca::class],
    version = 3,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
    ],
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun turmaDao(): TurmaDao
    abstract fun alunoDao(): AlunoDao
    abstract fun chamadaDao(): ChamadaDao

    companion object {
        const val NOME = "diario.db"
    }
}
