package br.com.ricardo.diariodeclasse.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import br.com.ricardo.diariodeclasse.data.local.entity.Aluno
import br.com.ricardo.diariodeclasse.data.local.entity.AlunoNaMeta
import br.com.ricardo.diariodeclasse.data.local.entity.Anotacao
import br.com.ricardo.diariodeclasse.data.local.entity.Chamada
import br.com.ricardo.diariodeclasse.data.local.entity.DadosDoDiario
import br.com.ricardo.diariodeclasse.data.local.entity.Foto
import br.com.ricardo.diariodeclasse.data.local.entity.Lembrete
import br.com.ricardo.diariodeclasse.data.local.entity.Meta
import br.com.ricardo.diariodeclasse.data.local.entity.Metrica
import br.com.ricardo.diariodeclasse.data.local.entity.NivelDaMetrica
import br.com.ricardo.diariodeclasse.data.local.entity.Pendencia
import br.com.ricardo.diariodeclasse.data.local.entity.Perfil
import br.com.ricardo.diariodeclasse.data.local.entity.RegistroPresenca
import br.com.ricardo.diariodeclasse.data.local.entity.ResultadoDaSondagem
import br.com.ricardo.diariodeclasse.data.local.entity.Sondagem
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

    @Query("SELECT * FROM metricas")
    suspend fun buscarMetricas(): List<Metrica>

    @Query("SELECT * FROM niveis_da_metrica")
    suspend fun buscarNiveisDaMetrica(): List<NivelDaMetrica>

    @Query("SELECT * FROM sondagens")
    suspend fun buscarSondagens(): List<Sondagem>

    @Query("SELECT * FROM resultados_da_sondagem")
    suspend fun buscarResultadosDaSondagem(): List<ResultadoDaSondagem>

    @Query("SELECT * FROM metas")
    suspend fun buscarMetas(): List<Meta>

    @Query("SELECT * FROM alunos_na_meta")
    suspend fun buscarAlunosNaMeta(): List<AlunoNaMeta>

    @Query("SELECT * FROM lembretes")
    suspend fun buscarLembretes(): List<Lembrete>

    @Query("SELECT * FROM fotos")
    suspend fun buscarFotos(): List<Foto>

    @Query("SELECT * FROM perfil")
    suspend fun buscarPerfis(): List<Perfil>

    @Query("DELETE FROM perfil")
    suspend fun apagarPerfis()

    @Query("DELETE FROM fotos")
    suspend fun apagarFotos()

    @Query("DELETE FROM lembretes")
    suspend fun apagarLembretes()

    @Query("DELETE FROM alunos_na_meta")
    suspend fun apagarAlunosNaMeta()

    @Query("DELETE FROM metas")
    suspend fun apagarMetas()

    @Query("DELETE FROM resultados_da_sondagem")
    suspend fun apagarResultadosDaSondagem()

    @Query("DELETE FROM sondagens")
    suspend fun apagarSondagens()

    @Query("DELETE FROM niveis_da_metrica")
    suspend fun apagarNiveisDaMetrica()

    @Query("DELETE FROM metricas")
    suspend fun apagarMetricas()

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

    @Insert
    suspend fun inserirMetricas(metricas: List<Metrica>)

    @Insert
    suspend fun inserirNiveisDaMetrica(niveis: List<NivelDaMetrica>)

    @Insert
    suspend fun inserirSondagens(sondagens: List<Sondagem>)

    @Insert
    suspend fun inserirResultadosDaSondagem(resultados: List<ResultadoDaSondagem>)

    @Insert
    suspend fun inserirMetas(metas: List<Meta>)

    @Insert
    suspend fun inserirAlunosNaMeta(alunos: List<AlunoNaMeta>)

    @Insert
    suspend fun inserirLembretes(lembretes: List<Lembrete>)

    @Insert
    suspend fun inserirFotos(fotos: List<Foto>)

    @Insert
    suspend fun inserirPerfis(perfis: List<Perfil>)

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
            metricas = buscarMetricas(),
            niveisDaMetrica = buscarNiveisDaMetrica(),
            sondagens = buscarSondagens(),
            resultadosDaSondagem = buscarResultadosDaSondagem(),
            metas = buscarMetas(),
            alunosNaMeta = buscarAlunosNaMeta(),
            lembretes = buscarLembretes(),
            fotos = buscarFotos(),
            perfis = buscarPerfis(),
        )
    }

    /**
     * Apaga tudo e grava os [dados] no lugar, numa única transação: se qualquer
     * passo falhar (ex.: arquivo com referência quebrada), nada é alterado.
     *
     * A ordem respeita as chaves estrangeiras: apaga primeiro quem aponta para
     * os outros (alunos na meta, metas, resultados...) e insere primeiro quem é
     * apontado (turmas, alunos...).
     */
    @Transaction
    suspend fun substituirTudo(dados: DadosDoDiario) {
        apagarPerfis()
        apagarFotos()
        apagarLembretes()
        apagarAlunosNaMeta()
        apagarMetas()
        apagarResultadosDaSondagem()
        apagarSondagens()
        apagarNiveisDaMetrica()
        apagarMetricas()
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
        inserirMetricas(dados.metricas)
        inserirNiveisDaMetrica(dados.niveisDaMetrica)
        inserirSondagens(dados.sondagens)
        inserirResultadosDaSondagem(dados.resultadosDaSondagem)
        inserirMetas(dados.metas)
        inserirAlunosNaMeta(dados.alunosNaMeta)
        inserirLembretes(dados.lembretes)
        inserirFotos(dados.fotos)
        inserirPerfis(dados.perfis)
    }
}
