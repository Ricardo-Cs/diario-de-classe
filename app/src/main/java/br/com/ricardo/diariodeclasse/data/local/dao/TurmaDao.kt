package br.com.ricardo.diariodeclasse.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import br.com.ricardo.diariodeclasse.data.local.entity.Turma
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Consultas que retornam [Flow] são "vivas": o Room emite de novo sempre que a tabela muda.
 * Funções `suspend` rodam fora da thread principal.
 * Todas as leituras filtram `deletedAt IS NULL` (soft delete).
 */
@Dao
interface TurmaDao {
    @Query("SELECT * FROM turmas WHERE deletedAt IS NULL ORDER BY anoLetivo DESC, nome COLLATE LOCALIZED")
    fun observarTodas(): Flow<List<Turma>>

    @Query("SELECT * FROM turmas WHERE id = :id AND deletedAt IS NULL")
    fun observarPorId(id: String): Flow<Turma?>

    @Query("SELECT * FROM turmas WHERE id = :id AND deletedAt IS NULL")
    suspend fun buscarPorId(id: String): Turma?

    @Insert
    suspend fun inserir(turma: Turma)

    @Update
    suspend fun atualizar(turma: Turma)

    @Query("UPDATE turmas SET deletedAt = :agora, updatedAt = :agora WHERE id = :id")
    suspend fun marcarComoExcluida(id: String, agora: Instant)
}
