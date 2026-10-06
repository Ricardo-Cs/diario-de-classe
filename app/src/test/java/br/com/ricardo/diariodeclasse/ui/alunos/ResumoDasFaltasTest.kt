package br.com.ricardo.diariodeclasse.ui.alunos

import br.com.ricardo.diariodeclasse.data.local.entity.FaltaDoAluno
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ResumoDasFaltasTest {
    private val hoje: LocalDate = LocalDate.of(2026, 10, 6)

    private fun faltaEm(data: LocalDate): FaltaDoAluno {
        return FaltaDoAluno(data = data, observacao = null)
    }

    @Test
    fun semFaltas() {
        val resumo = calcularResumoDasFaltas(emptyList(), hoje)

        assertEquals(0, resumo.noMes)
        assertEquals(0, resumo.recentes.size)
        assertEquals(0, resumo.naoMostradas)
    }

    @Test
    fun contaNoMesSoAsFaltasDoMesCorrente() {
        val faltas = listOf(
            faltaEm(LocalDate.of(2026, 10, 5)),
            faltaEm(LocalDate.of(2026, 10, 1)),
            faltaEm(LocalDate.of(2026, 9, 30)),
            faltaEm(LocalDate.of(2025, 10, 15)),
        )

        val resumo = calcularResumoDasFaltas(faltas, hoje)

        assertEquals(2, resumo.noMes)
    }

    @Test
    fun mostraNoMaximoAsCincoMaisRecentes() {
        val faltas = mutableListOf<FaltaDoAluno>()
        for (diasAtras in 1..7) {
            faltas.add(faltaEm(hoje.minusDays(diasAtras.toLong())))
        }

        val resumo = calcularResumoDasFaltas(faltas, hoje)

        assertEquals(MAXIMO_DE_FALTAS_NA_TELA_DO_ALUNO, resumo.recentes.size)
        assertEquals(hoje.minusDays(1), resumo.recentes.first().data)
        assertEquals(2, resumo.naoMostradas)
    }
}
