package br.com.ricardo.diariodeclasse.ui.inicio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalTime

class SaudacaoTest {

    @Test
    fun manha_bomDia() {
        assertEquals(Saudacao.BOM_DIA, saudacaoParaHorario(LocalTime.of(0, 0)))
        assertEquals(Saudacao.BOM_DIA, saudacaoParaHorario(LocalTime.of(11, 59)))
    }

    @Test
    fun tarde_boaTarde() {
        assertEquals(Saudacao.BOA_TARDE, saudacaoParaHorario(LocalTime.of(12, 0)))
        assertEquals(Saudacao.BOA_TARDE, saudacaoParaHorario(LocalTime.of(17, 59)))
    }

    @Test
    fun noite_boaNoite() {
        assertEquals(Saudacao.BOA_NOITE, saudacaoParaHorario(LocalTime.of(18, 0)))
        assertEquals(Saudacao.BOA_NOITE, saudacaoParaHorario(LocalTime.of(23, 59)))
    }

    @Test
    fun primeiroNome_ficaSoComOPrimeiro() {
        assertEquals("Maria", primeiroNome("  Maria Souza Lima "))
        assertEquals("Maria", primeiroNome("Maria"))
    }

    @Test
    fun primeiroNome_semNomeENulo() {
        assertNull(primeiroNome(null))
        assertNull(primeiroNome("   "))
    }
}
