package br.com.ricardo.diariodeclasse.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import br.com.ricardo.diariodeclasse.data.local.entity.Lembrete
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

@Dao
interface LembreteDao {
    /** Os mais urgentes (inclusive atrasados) primeiro. */
    @Query(
        "SELECT * FROM lembretes WHERE concluidoEm IS NULL AND deletedAt IS NULL " +
            "ORDER BY data, createdAt"
    )
    fun observarEmAberto(): Flow<List<Lembrete>>

    /** Concluídos mais recentes primeiro; o [limite] evita uma lista que só cresce. */
    @Query(
        "SELECT * FROM lembretes WHERE concluidoEm IS NOT NULL AND deletedAt IS NULL " +
            "ORDER BY concluidoEm DESC LIMIT :limite"
    )
    fun observarConcluidos(limite: Int): Flow<List<Lembrete>>

    /** Em aberto com data até [data] (o dia e os atrasados), para a notificação diária. */
    @Query(
        "SELECT * FROM lembretes WHERE concluidoEm IS NULL AND deletedAt IS NULL AND data <= :data " +
            "ORDER BY data, createdAt"
    )
    suspend fun buscarAte(data: LocalDate): List<Lembrete>

    /** Inclui os excluídos: é usada também para desfazer uma exclusão. */
    @Query("SELECT * FROM lembretes WHERE id = :id")
    suspend fun buscarPorId(id: String): Lembrete?

    @Insert
    suspend fun inserir(lembrete: Lembrete)

    @Update
    suspend fun atualizar(lembrete: Lembrete)

    @Query("UPDATE lembretes SET deletedAt = :agora, updatedAt = :agora WHERE id = :id")
    suspend fun marcarComoExcluido(id: String, agora: Instant)
}
