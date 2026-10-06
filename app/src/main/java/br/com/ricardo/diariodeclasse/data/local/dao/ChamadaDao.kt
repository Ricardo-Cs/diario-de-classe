package br.com.ricardo.diariodeclasse.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import br.com.ricardo.diariodeclasse.data.local.entity.Chamada
import br.com.ricardo.diariodeclasse.data.local.entity.RegistroPresenca
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * `@Upsert` insere a linha ou, se o id já existe, atualiza: serve tanto para
 * a primeira chamada do dia quanto para a edição.
 */
@Dao
interface ChamadaDao {
    @Query("SELECT * FROM chamadas WHERE turmaId = :turmaId AND data = :data AND deletedAt IS NULL")
    fun observarChamada(turmaId: String, data: LocalDate): Flow<Chamada?>

    @Query(
        "SELECT registros_presenca.* FROM registros_presenca " +
            "INNER JOIN chamadas ON chamadas.id = registros_presenca.chamadaId " +
            "WHERE chamadas.turmaId = :turmaId AND chamadas.data = :data " +
            "AND chamadas.deletedAt IS NULL AND registros_presenca.deletedAt IS NULL"
    )
    fun observarRegistrosDaChamada(turmaId: String, data: LocalDate): Flow<List<RegistroPresenca>>

    @Upsert
    suspend fun salvarChamada(chamada: Chamada)

    @Upsert
    suspend fun salvarRegistros(registros: List<RegistroPresenca>)

    /**
     * `@Transaction` grava tudo ou nada: se algo falhar no meio, a chamada
     * não fica pela metade (como o `@Transactional` do Spring).
     */
    @Transaction
    suspend fun salvarChamadaComRegistros(chamada: Chamada, registros: List<RegistroPresenca>) {
        salvarChamada(chamada)
        salvarRegistros(registros)
    }
}
