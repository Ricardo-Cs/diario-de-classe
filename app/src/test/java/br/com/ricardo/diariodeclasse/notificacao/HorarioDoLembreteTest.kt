package br.com.ricardo.diariodeclasse.notificacao

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class HorarioDoLembreteTest {
    // 06/10/2026 é uma terça-feira.
    private val terca = LocalDate.of(2026, 10, 6)
    private val segunda = LocalDate.of(2026, 10, 5)
    private val sabado = LocalDate.of(2026, 10, 10)

    @Test
    fun antesDasSete_naoAvisa() {
        val agora: LocalDateTime = terca.atTime(6, 59)

        assertFalse(deveAvisarAgora(agora, ultimoDiaAvisado = segunda))
    }

    @Test
    fun aPartirDasSete_avisa() {
        val agora: LocalDateTime = terca.atTime(7, 0)

        assertTrue(deveAvisarAgora(agora, ultimoDiaAvisado = segunda))
    }

    @Test
    fun nuncaAvisouAntes_avisa() {
        val agora: LocalDateTime = terca.atTime(9, 30)

        assertTrue(deveAvisarAgora(agora, ultimoDiaAvisado = null))
    }

    @Test
    fun jaAvisouHoje_naoAvisaDeNovo() {
        val agora: LocalDateTime = terca.atTime(15, 0)

        assertFalse(deveAvisarAgora(agora, ultimoDiaAvisado = terca))
    }

    @Test
    fun celularDesligadoDeManha_avisaQuandoVoltar() {
        val agora: LocalDateTime = terca.atTime(13, 0)

        assertTrue(deveAvisarAgora(agora, ultimoDiaAvisado = segunda))
    }

    @Test
    fun fimDeSemana_naoAvisa() {
        val agora: LocalDateTime = sabado.atTime(9, 0)

        assertFalse(deveAvisarAgora(agora, ultimoDiaAvisado = LocalDate.of(2026, 10, 9)))
    }
}
