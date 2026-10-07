package br.com.ricardo.diariodeclasse.ui.metricas

import br.com.ricardo.diariodeclasse.data.local.entity.Aluno
import br.com.ricardo.diariodeclasse.data.local.entity.NivelDaMetrica
import br.com.ricardo.diariodeclasse.data.local.entity.ResultadoDatado
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class NiveisAtuaisTest {
    private val agora: Instant = Instant.parse("2026-10-06T11:00:00Z")
    private val hoje: LocalDate = LocalDate.of(2026, 10, 6)

    private val ana = Aluno(id = "ana", turmaId = "turma", nome = "Ana", createdAt = agora, updatedAt = agora)
    private val bruno = Aluno(id = "bruno", turmaId = "turma", nome = "Bruno", createdAt = agora, updatedAt = agora)
    private val carla = Aluno(id = "carla", turmaId = "turma", nome = "Carla", createdAt = agora, updatedAt = agora)

    private val silabico = nivel("silabico", 0)
    private val alfabetico = nivel("alfabetico", 1)

    private fun nivel(id: String, ordem: Int): NivelDaMetrica {
        return NivelDaMetrica(
            id = id, metricaId = "escrita", nome = id, ordem = ordem,
            createdAt = agora, updatedAt = agora,
        )
    }

    private fun resultado(alunoId: String, nivelId: String, data: LocalDate): ResultadoDatado {
        return ResultadoDatado(metricaId = "escrita", alunoId = alunoId, nivelId = nivelId, data = data)
    }

    @Test
    fun vale_oResultadoDaSondagemMaisRecente() {
        val resultados = listOf(
            resultado("ana", "alfabetico", hoje.minusDays(5)),
            resultado("ana", "silabico", hoje.minusDays(30)),
        )

        val atuais = nivelAtualDeCadaAluno(resultados, hoje)

        assertEquals("alfabetico", atuais.getValue("ana").nivelId)
    }

    @Test
    fun ignora_sondagensDepoisDaDataPedida() {
        val resultados = listOf(
            resultado("ana", "silabico", hoje.minusDays(30)),
            resultado("ana", "alfabetico", hoje.minusDays(5)),
        )

        val atuais = nivelAtualDeCadaAluno(resultados, hoje.minusDays(10))

        assertEquals("silabico", atuais.getValue("ana").nivelId)
    }

    @Test
    fun alunoSemResultado_naoAparece() {
        val atuais = nivelAtualDeCadaAluno(listOf(resultado("ana", "silabico", hoje)), hoje)

        assertNull(atuais["bruno"])
    }

    @Test
    fun distribuicao_contaPorNivelEOsSemAvaliacao() {
        val atuais = nivelAtualDeCadaAluno(
            listOf(
                resultado("ana", "alfabetico", hoje),
                resultado("bruno", "alfabetico", hoje),
            ),
            hoje,
        )

        val distribuicao = calcularDistribuicao(listOf(silabico, alfabetico), listOf(ana, bruno, carla), atuais)

        assertEquals(listOf(0, 2), distribuicao.faixas.map { faixa -> faixa.quantidade })
        assertEquals(1, distribuicao.semAvaliacao)
    }

    @Test
    fun distribuicao_ignoraAlunoQueNaoEstaMaisNaTurma() {
        val atuais = nivelAtualDeCadaAluno(listOf(resultado("excluido", "silabico", hoje)), hoje)

        val distribuicao = calcularDistribuicao(listOf(silabico, alfabetico), listOf(ana), atuais)

        assertEquals(listOf(0, 0), distribuicao.faixas.map { faixa -> faixa.quantidade })
        assertEquals(1, distribuicao.semAvaliacao)
    }
}
