package br.com.ricardo.diariodeclasse.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import br.com.ricardo.diariodeclasse.data.local.entity.Perfil
import br.com.ricardo.diariodeclasse.data.local.entity.ResumoDoPerfil
import kotlinx.coroutines.flow.Flow

@Dao
interface PerfilDao {
    /** O perfil é único; o `LIMIT 1` só garante isso se um dia houver mais de uma linha. */
    @Query("SELECT * FROM perfil WHERE deletedAt IS NULL ORDER BY createdAt LIMIT 1")
    fun observar(): Flow<Perfil?>

    @Query("SELECT * FROM perfil WHERE deletedAt IS NULL ORDER BY createdAt LIMIT 1")
    suspend fun buscar(): Perfil?

    /**
     * As três contagens numa consulta só. Alunos e anotações de turmas (ou alunos)
     * excluídos não contam, como no resto do app.
     */
    @Query(
        "SELECT " +
            "(SELECT COUNT(*) FROM turmas WHERE deletedAt IS NULL) AS turmas, " +
            "(SELECT COUNT(*) FROM alunos " +
            "INNER JOIN turmas ON turmas.id = alunos.turmaId " +
            "WHERE alunos.deletedAt IS NULL AND turmas.deletedAt IS NULL) AS alunos, " +
            "(SELECT COUNT(*) FROM anotacoes " +
            "INNER JOIN alunos ON alunos.id = anotacoes.alunoId " +
            "INNER JOIN turmas ON turmas.id = alunos.turmaId " +
            "WHERE anotacoes.deletedAt IS NULL AND alunos.deletedAt IS NULL AND turmas.deletedAt IS NULL) AS anotacoes"
    )
    fun observarResumo(): Flow<ResumoDoPerfil>

    @Insert
    suspend fun inserir(perfil: Perfil)

    @Update
    suspend fun atualizar(perfil: Perfil)
}
