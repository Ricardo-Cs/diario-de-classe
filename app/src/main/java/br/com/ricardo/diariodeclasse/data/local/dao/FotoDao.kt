package br.com.ricardo.diariodeclasse.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import br.com.ricardo.diariodeclasse.data.local.entity.Foto
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface FotoDao {
    /** Dias mais recentes primeiro; dentro do dia, na ordem em que foram adicionadas. */
    @Query(
        "SELECT * FROM fotos WHERE turmaId = :turmaId AND deletedAt IS NULL " +
            "ORDER BY data DESC, createdAt, id"
    )
    fun observarDaTurma(turmaId: String): Flow<List<Foto>>

    /** Inclui as excluídas (a edição da legenda lê a linha inteira para atualizá-la). */
    @Query("SELECT * FROM fotos WHERE id = :id")
    suspend fun buscarPorId(id: String): Foto?

    @Insert
    suspend fun inserir(foto: Foto)

    @Update
    suspend fun atualizar(foto: Foto)

    @Query("UPDATE fotos SET deletedAt = :agora, updatedAt = :agora WHERE id = :id")
    suspend fun marcarComoExcluida(id: String, agora: Instant)
}
