package br.com.ricardo.diariodeclasse.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import br.com.ricardo.diariodeclasse.data.local.entity.Aluno
import br.com.ricardo.diariodeclasse.data.local.entity.Anotacao
import br.com.ricardo.diariodeclasse.data.local.entity.Chamada
import br.com.ricardo.diariodeclasse.data.local.entity.DadosDoDiario
import br.com.ricardo.diariodeclasse.data.local.entity.Pendencia
import br.com.ricardo.diariodeclasse.data.local.entity.RegistroPresenca
import br.com.ricardo.diariodeclasse.data.local.entity.Turma

/**
 * Leitura e gravação do banco inteiro, para exportar e importar.
 *
 * Diferente dos outros DAOs, as consultas daqui NÃO filtram `deletedAt`: o
 * arquivo leva também o que foi excluído, porque outras linhas podem apontar
 * para elas (ex.: pendência de uma falta de aluno excluído) e porque a futura
 * sincronização precisa saber o que foi apagado.
 */
@Dao
interface BackupDao {
    @Query("SELECT * FROM turmas")
    suspend fun buscarTurmas(): List<Turma>

    @Query("SELECT * FROM alunos")
    suspend fun buscarAlunos(): List<Aluno>

    @Query("SELECT * FROM chamadas")
    suspend fun buscarChamadas(): List<Chamada>

    @Query("SELECT * FROM registros_presenca")
    suspend fun buscarRegistrosPresenca(): List<RegistroPresenca>

    @Query("SELECT * FROM pendencias")
    suspend fun buscarPendencias(): List<Pendencia>

    @Query("SELECT * FROM anotacoes")
    suspend fun buscarAnotacoes(): List<Anotacao>

    @Query("DELETE FROM anotacoes")
    suspend fun apagarAnotacoes()

    @Query("DELETE FROM pendencias")
    suspend fun apagarPendencias()

    @Query("DELETE FROM registros_presenca")
    suspend fun apagarRegistrosPresenca()

    @Query("DELETE FROM chamadas")
    suspend fun apagarChamadas()

    @Query("DELETE FROM alunos")
    suspend fun apagarAlunos()

    @Query("DELETE FROM turmas")
    suspend fun apagarTurmas()

    @Insert
    suspend fun inserirTurmas(turmas: List<Turma>)

    @Insert
    suspend fun inserirAlunos(alunos: List<Aluno>)

    @Insert
    suspend fun inserirChamadas(chamadas: List<Chamada>)

    @Insert
    suspend fun inserirRegistrosPresenca(registros: List<RegistroPresenca>)

    @Insert
    suspend fun inserirPendencias(pendencias: List<Pendencia>)

    @Insert
    suspend fun inserirAnotacoes(anotacoes: List<Anotacao>)

    /**
     * Lê todas as tabelas dentro de uma transação, para o retrato ser consistente
     * mesmo que algo seja gravado no meio da leitura.
     */
    @Transaction
    suspend fun lerTudo(): DadosDoDiario {
        return DadosDoDiario(
            turmas = buscarTurmas(),
            alunos = buscarAlunos(),
            chamadas = buscarChamadas(),
            registrosPresenca = buscarRegistrosPresenca(),
            pendencias = buscarPendencias(),
            anotacoes = buscarAnotacoes(),
        )
    }

    /**
     * Apaga tudo e grava os [dados] no lugar, numa única transação: se qualquer
     * passo falhar (ex.: arquivo com referência quebrada), nada é alterado.
     *
     * A ordem respeita as chaves estrangeiras: apaga primeiro quem aponta para
     * os outros (anotações, pendências...) e insere primeiro quem é apontado
     * (turmas, alunos...).
     */
    @Transaction
    suspend fun substituirTudo(dados: DadosDoDiario) {
        apagarAnotacoes()
        apagarPendencias()
        apagarRegistrosPresenca()
        apagarChamadas()
        apagarAlunos()
        apagarTurmas()

        inserirTurmas(dados.turmas)
        inserirAlunos(dados.alunos)
        inserirChamadas(dados.chamadas)
        inserirRegistrosPresenca(dados.registrosPresenca)
        inserirPendencias(dados.pendencias)
        inserirAnotacoes(dados.anotacoes)
    }
}
