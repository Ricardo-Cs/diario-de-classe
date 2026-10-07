package br.com.ricardo.diariodeclasse.ui.alunos

import br.com.ricardo.diariodeclasse.data.local.entity.NivelRegistradoDoAluno
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class EvolucaoDoAlunoTest {
    private val hoje: LocalDate = LocalDate.of(2026, 10, 6)
    private val agosto: LocalDate = LocalDate.of(2026, 8, 3)
    private val setembro: LocalDate = LocalDate.of(2026, 9, 15)
    private val outubro: LocalDate = LocalDate.of(2026, 10, 1)

    private fun registro(nivelId: String, data: LocalDate, metricaId: String = "escrita"): NivelRegistradoDoAluno {
        return NivelRegistradoDoAluno(
            metricaId = metricaId,
            nomeDaMetrica = "Métrica $metricaId",
            nivelId = nivelId,
            nomeDoNivel = "Nível $nivelId",
            data = data,
        )
    }

    @Test
    fun sondagensSeguidasNoMesmoNivel_viramUmPassoDatadoDaPrimeira() {
        val registros = listOf(
            registro("com-valor", outubro),
            registro("com-valor", setembro),
            registro("sem-valor", agosto),
        )

        val evolucao = calcularEvolucaoDoAluno(registros, hoje).single()

        assertEquals(
            listOf(PassoDaEvolucao("Nível com-valor", setembro), PassoDaEvolucao("Nível sem-valor", agosto)),
            evolucao.passos,
        )
    }

    @Test
    fun voltarAUmNivelAnterior_geraUmPassoNovo() {
        val registros = listOf(
            registro("sem-valor", outubro),
            registro("com-valor", setembro),
            registro("sem-valor", agosto),
        )

        val passos = calcularEvolucaoDoAluno(registros, hoje).single().passos

        assertEquals(listOf(outubro, setembro, agosto), passos.map { passo -> passo.desde })
    }

    @Test
    fun separaPorMetricaNaOrdemRecebida() {
        val registros = listOf(
            registro("a", setembro, metricaId = "escrita"),
            registro("b", setembro, metricaId = "matematica"),
        )

        val evolucoes = calcularEvolucaoDoAluno(registros, hoje)

        assertEquals(listOf("escrita", "matematica"), evolucoes.map { evolucao -> evolucao.metricaId })
    }

    @Test
    fun ignoraSondagemComDataFutura() {
        val registros = listOf(registro("alfabetico", hoje.plusDays(1)), registro("com-valor", setembro))

        val passos = calcularEvolucaoDoAluno(registros, hoje).single().passos

        assertEquals(listOf(PassoDaEvolucao("Nível com-valor", setembro)), passos)
    }
}
