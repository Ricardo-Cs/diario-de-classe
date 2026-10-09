package br.com.ricardo.diariodeclasse.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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
import br.com.ricardo.diariodeclasse.data.local.entity.Aluno
import br.com.ricardo.diariodeclasse.data.local.entity.AlunoNaMeta
import br.com.ricardo.diariodeclasse.data.local.entity.Anotacao
import br.com.ricardo.diariodeclasse.data.local.entity.Chamada
import br.com.ricardo.diariodeclasse.data.local.entity.Foto
import br.com.ricardo.diariodeclasse.data.local.entity.Lembrete
import br.com.ricardo.diariodeclasse.data.local.entity.Meta
import br.com.ricardo.diariodeclasse.data.local.entity.Metrica
import br.com.ricardo.diariodeclasse.data.local.entity.NivelDaMetrica
import br.com.ricardo.diariodeclasse.data.local.entity.Pendencia
import br.com.ricardo.diariodeclasse.data.local.entity.RegistroPresenca
import br.com.ricardo.diariodeclasse.data.local.entity.ResultadoDaSondagem
import br.com.ricardo.diariodeclasse.data.local.entity.Sondagem
import br.com.ricardo.diariodeclasse.data.local.entity.Turma

/**
 * Ao adicionar ou alterar entidades, incremente [version] e declare a migração:
 * o app substitui o caderno, então nunca usar `fallbackToDestructiveMigration`.
 *
 * Histórico:
 * 1 → turmas
 * 2 → alunos
 * 3 → chamadas e registros de presença
 * 4 → pendências
 * 5 → anotações
 * 6 → métricas (com níveis e sondagens) e metas
 * 7 → metas livres: métrica, nível-alvo e prazo opcionais; aluno na meta ganha "atingiu em"
 * 8 → lembretes da professora
 * 9 → fotos do registro do dia
 */
@Database(
    entities = [
        Turma::class,
        Aluno::class,
        Chamada::class,
        RegistroPresenca::class,
        Pendencia::class,
        Anotacao::class,
        Metrica::class,
        NivelDaMetrica::class,
        Sondagem::class,
        ResultadoDaSondagem::class,
        Meta::class,
        AlunoNaMeta::class,
        Lembrete::class,
        Foto::class,
    ],
    version = 9,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6),
        // Tornar colunas opcionais exige recriar a tabela no SQLite; o Room gera
        // essa cópia sozinho (cria a tabela nova, copia as linhas e troca as duas).
        AutoMigration(from = 6, to = 7),
        AutoMigration(from = 7, to = 8),
        AutoMigration(from = 8, to = 9),
    ],
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun turmaDao(): TurmaDao
    abstract fun alunoDao(): AlunoDao
    abstract fun chamadaDao(): ChamadaDao
    abstract fun pendenciaDao(): PendenciaDao
    abstract fun anotacaoDao(): AnotacaoDao
    abstract fun metricaDao(): MetricaDao
    abstract fun metaDao(): MetaDao
    abstract fun lembreteDao(): LembreteDao
    abstract fun fotoDao(): FotoDao
    abstract fun backupDao(): BackupDao

    companion object {
        const val NOME = "diario.db"
    }
}
