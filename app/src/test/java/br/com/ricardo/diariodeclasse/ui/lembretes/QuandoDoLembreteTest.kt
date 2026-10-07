package br.com.ricardo.diariodeclasse.ui.lembretes

import br.com.ricardo.diariodeclasse.data.local.entity.Lembrete
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class QuandoDoLembreteTest {
    private val agora: Instant = Instant.parse("2026-10-06T11:00:00Z")
    private val hoje: LocalDate = LocalDate.of(2026, 10, 6)

    private fun lembrete(id: String, data: LocalDate): Lembrete {
        return Lembrete(id = id, descricao = "Lembrete $id", data = data, createdAt = agora, updatedAt = agora)
    }

    @Test
    fun quando_classificaPelaDistanciaDeHoje() {
        assertEquals(QuandoDoLembrete.Atrasado(dias = 3), quandoDoLembrete(hoje.minusDays(3), hoje))
        assertEquals(QuandoDoLembrete.Hoje, quandoDoLembrete(hoje, hoje))
        assertEquals(QuandoDoLembrete.Amanha, quandoDoLembrete(hoje.plusDays(1), hoje))
        assertEquals(QuandoDoLembrete.EmDias(dias = 31), quandoDoLembrete(hoje.plusDays(31), hoje))
    }

    @Test
    fun inicio_mostraAtrasadosHojeEOsProximosSeteDias() {
        val emAberto = listOf(
            lembrete("atrasado", hoje.minusDays(10)),
            lembrete("hoje", hoje),
            lembrete("daqui-7-dias", hoje.plusDays(7)),
            lembrete("daqui-8-dias", hoje.plusDays(8)),
        )

        val doInicio: List<Lembrete> = lembretesDoInicio(emAberto, hoje)

        assertEquals(listOf("atrasado", "hoje", "daqui-7-dias"), doInicio.map { item -> item.id })
    }
}
