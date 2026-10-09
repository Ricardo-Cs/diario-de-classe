package br.com.ricardo.diariodeclasse.ui.fotos

import br.com.ricardo.diariodeclasse.data.local.entity.Foto
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.time.Instant
import java.time.LocalDate

class DiasDeFotosTest {
    private val agora: Instant = Instant.parse("2026-10-09T11:00:00Z")
    private val hoje: LocalDate = LocalDate.of(2026, 10, 9)

    private fun foto(id: String, data: LocalDate): FotoNaTela {
        val foto = Foto(
            id = id, turmaId = "turma", data = data, nomeDoArquivo = "$id.jpg",
            createdAt = agora, updatedAt = agora,
        )
        return FotoNaTela(foto, File("$id.jpg"))
    }

    @Test
    fun agruparPorDia_mantemAOrdemRecebida() {
        val fotos: List<FotoNaTela> = listOf(
            foto("a", hoje),
            foto("b", hoje),
            foto("c", hoje.minusDays(3)),
        )

        val dias: List<DiaDeFotos> = agruparPorDia(fotos)

        assertEquals(listOf(hoje, hoje.minusDays(3)), dias.map { dia -> dia.data })
        assertEquals(listOf("a", "b"), dias[0].fotos.map { foto -> foto.foto.id })
        assertEquals(listOf("c"), dias[1].fotos.map { foto -> foto.foto.id })
    }

    @Test
    fun agruparPorDia_semFotos() {
        assertEquals(emptyList<DiaDeFotos>(), agruparPorDia(emptyList()))
    }
}
