package br.com.ricardo.diariodeclasse.ui.perfil

import org.junit.Assert.assertEquals
import org.junit.Test

class IniciaisDoNomeTest {

    @Test
    fun nomeComSobrenome_primeiraEUltimaIniciais() {
        assertEquals("MS", iniciaisDoNome("maria da silva souza"))
    }

    @Test
    fun soUmNome_umaInicial() {
        assertEquals("M", iniciaisDoNome("  Maria "))
    }

    @Test
    fun semNome_vazio() {
        assertEquals("", iniciaisDoNome("   "))
    }
}
