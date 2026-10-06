package br.com.ricardo.diariodeclasse.ui.chamada

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class LembreteDaFaltaTest {
    private val hoje = LocalDate.of(2026, 10, 6)

    @Test
    fun chamadaDeHoje_lembraAmanha() {
        assertEquals(hoje.plusDays(1), lembreteInicialDaFalta(dataDaChamada = hoje, hoje = hoje))
    }

    @Test
    fun chamadaDeOntem_lembraHoje() {
        assertEquals(hoje, lembreteInicialDaFalta(dataDaChamada = hoje.minusDays(1), hoje = hoje))
    }

    @Test
    fun chamadaDaSemanaPassada_lembraHoje() {
        assertEquals(hoje, lembreteInicialDaFalta(dataDaChamada = hoje.minusDays(7), hoje = hoje))
    }
}
