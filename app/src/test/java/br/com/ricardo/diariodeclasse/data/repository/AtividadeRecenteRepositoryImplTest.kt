package br.com.ricardo.diariodeclasse.data.repository

import br.com.ricardo.diariodeclasse.data.local.dao.AtividadeRecenteDao
import br.com.ricardo.diariodeclasse.data.local.dao.LinhaDeAtividade
import br.com.ricardo.diariodeclasse.data.local.entity.StatusPendencia
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/** Devolve linhas prontas: a consulta SQL em si é validada pelo Room na compilação. */
private class FakeAtividadeRecenteDao(private val linhas: List<LinhaDeAtividade>) : AtividadeRecenteDao {
    override fun observarDaTurma(turmaId: String, limite: Int): Flow<List<LinhaDeAtividade>> {
        return flowOf(linhas)
    }
}

class AtividadeRecenteRepositoryImplTest {
    private val fusoDeSaoPaulo: ZoneId = ZoneId.of("America/Sao_Paulo")
    private val relogio: Clock = Clock.fixed(Instant.parse("2026-10-06T15:00:00Z"), fusoDeSaoPaulo)

    // 13:00 UTC = 10:00 em São Paulo
    private val criado: Instant = Instant.parse("2026-10-06T13:00:00Z")
    private val umaHoraDepois: Instant = criado.plusSeconds(3600)

    private fun linha(
        origem: String,
        momento: Instant = criado,
        status: StatusPendencia? = null,
        nomeDoAluno: String? = null,
        texto: String? = null,
        ausentes: Int? = null,
    ): LinhaDeAtividade {
        return LinhaDeAtividade(
            origem = origem,
            id = "id",
            momento = momento,
            criadoEm = criado,
            status = status,
            nomeDoAluno = nomeDoAluno,
            texto = texto,
            ausentes = ausentes,
        )
    }

    private fun converter(linha: LinhaDeAtividade): AtividadeRecente = runBlocking {
        val repositorio = AtividadeRecenteRepositoryImpl(FakeAtividadeRecenteDao(listOf(linha)), relogio)
        repositorio.observarDaTurma("turma", 5).first().single()
    }

    @Test
    fun chamadaNova_noHorarioLocal() {
        val atividade = converter(linha("CHAMADA", ausentes = 2))

        val esperado = AtividadeRecente.Chamada(LocalDateTime.of(2026, 10, 6, 10, 0), editada = false, ausentes = 2)
        assertEquals(esperado, atividade)
    }

    @Test
    fun chamadaAlteradaDepois_eEditada() {
        val atividade = converter(linha("CHAMADA", momento = umaHoraDepois, ausentes = 0)) as AtividadeRecente.Chamada

        assertEquals(true, atividade.editada)
    }

    @Test
    fun pendenciaEntregue() {
        val atividade = converter(
            linha("PENDENCIA", momento = umaHoraDepois, status = StatusPendencia.ENTREGUE, nomeDoAluno = "Maria", texto = "Ficha")
        )

        val esperado = AtividadeRecente.PendenciaEntregue(LocalDateTime.of(2026, 10, 6, 11, 0), "Maria", "Ficha")
        assertEquals(esperado, atividade)
    }

    @Test
    fun pendenciaPendente_eRegistrada() {
        val atividade = converter(
            linha("PENDENCIA", status = StatusPendencia.PENDENTE, nomeDoAluno = "Maria", texto = "Ficha")
        )

        val esperado = AtividadeRecente.PendenciaRegistrada(LocalDateTime.of(2026, 10, 6, 10, 0), false, "Maria", "Ficha")
        assertEquals(esperado, atividade)
    }

    @Test
    fun anotacao() {
        val atividade = converter(linha("ANOTACAO", nomeDoAluno = "Alan", texto = "Leu sozinho"))

        val esperado = AtividadeRecente.Anotacao(LocalDateTime.of(2026, 10, 6, 10, 0), false, "Alan", "Leu sozinho")
        assertEquals(esperado, atividade)
    }
}
