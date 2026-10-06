package br.com.ricardo.diariodeclasse.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import br.com.ricardo.diariodeclasse.data.local.entity.Anotacao
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface AnotacaoDao {
    /** Mais recentes primeiro: é o que a professora quer ver ao abrir o aluno. */
    @Query(
        "SELECT * FROM anotacoes WHERE alunoId = :alunoId AND deletedAt IS NULL " +
            "ORDER BY data DESC, createdAt DESC"
    )
    fun observarDoAluno(alunoId: String): Flow<List<Anotacao>>

    /** Inclui as excluídas: é usada também para desfazer uma exclusão. */
    @Query("SELECT * FROM anotacoes WHERE id = :id")
    suspend fun buscarPorId(id: String): Anotacao?

    @Insert
    suspend fun inserir(anotacao: Anotacao)

    @Update
    suspend fun atualizar(anotacao: Anotacao)

    @Query("UPDATE anotacoes SET deletedAt = :agora, updatedAt = :agora WHERE id = :id")
    suspend fun marcarComoExcluida(id: String, agora: Instant)
}
