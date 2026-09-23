package br.com.ricardo.diariodeclasse.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import br.com.ricardo.diariodeclasse.data.local.entity.Aluno
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/** `COLLATE LOCALIZED` ordena respeitando acentos ("Álvaro" antes de "Bruno"). */
@Dao
interface AlunoDao {
    @Query(
        "SELECT * FROM alunos WHERE turmaId = :turmaId AND deletedAt IS NULL " +
            "ORDER BY nome COLLATE LOCALIZED"
    )
    fun observarDaTurma(turmaId: String): Flow<List<Aluno>>

    @Insert
    suspend fun inserir(aluno: Aluno)

    @Update
    suspend fun atualizar(aluno: Aluno)

    @Query("UPDATE alunos SET deletedAt = :agora, updatedAt = :agora WHERE id = :id")
    suspend fun marcarComoExcluido(id: String, agora: Instant)
}
