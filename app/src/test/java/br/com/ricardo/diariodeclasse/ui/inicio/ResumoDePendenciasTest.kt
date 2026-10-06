package br.com.ricardo.diariodeclasse.ui.inicio

import br.com.ricardo.diariodeclasse.data.local.entity.Aluno
import br.com.ricardo.diariodeclasse.data.local.entity.Pendencia
import br.com.ricardo.diariodeclasse.data.local.entity.PendenciaComOrigem
import br.com.ricardo.diariodeclasse.data.local.entity.StatusPendencia
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class ResumoDePendenciasTest {
    private val agora: Instant = Instant.parse("2026-10-06T11:00:00Z")
    private val hoje: LocalDate = LocalDate.of(2026, 10, 6)

    private val ana = Aluno(id = "ana", turmaId = "turma", nome = "Ana", createdAt = agora, updatedAt = agora)
    private val bruno = Aluno(id = "bruno", turmaId = "turma", nome = "Bruno", createdAt = agora, updatedAt = agora)

    private fun pendencia(
        id: String,
        alunoId: String,
        dataLembrete: LocalDate,
        dataDaFalta: LocalDate? = null,
    ): PendenciaComOrigem {
        val pendencia = Pendencia(
            id = id,
            alunoId = alunoId,
            descricao = "Atividade $id",
            dataLembrete = dataLembrete,
            status = StatusPendencia.PENDENTE,
            createdAt = agora,
            updatedAt = agora,
        )
        return PendenciaComOrigem(pendencia, dataDaFalta)
    }

    @Test
    fun semPendencias() {
        val resumo = calcularResumoDePendencias(listOf(ana), emptyList(), hoje)

        assertEquals(ResumoDePendencias(paraHoje = 0, emAberto = 0, lembretes = emptyList()), resumo)
    }

    @Test
    fun separaAsDeHojeDasFuturas() {
        val pendencias = listOf(
            pendencia("1", "ana", hoje.minusDays(2)),
            pendencia("2", "bruno", hoje),
            pendencia("3", "ana", hoje.plusDays(1)),
        )

        val resumo = calcularResumoDePendencias(listOf(ana, bruno), pendencias, hoje)

        assertEquals(2, resumo.paraHoje)
        assertEquals(3, resumo.emAberto)
        assertEquals(listOf("1", "2"), resumo.lembretes.map { lembrete -> lembrete.pendenciaId })
    }

    @Test
    fun mostraNoMaximoTresLembretes() {
        val pendencias = listOf(
            pendencia("1", "ana", hoje),
            pendencia("2", "ana", hoje),
            pendencia("3", "bruno", hoje),
            pendencia("4", "bruno", hoje),
        )

        val resumo = calcularResumoDePendencias(listOf(ana, bruno), pendencias, hoje)

        assertEquals(4, resumo.paraHoje)
        assertEquals(MAXIMO_DE_LEMBRETES_NO_INICIO, resumo.lembretes.size)
    }

    @Test
    fun lembreteTrazNomeDoAlunoEOrigem() {
        val dataDaFalta = hoje.minusDays(1)
        val pendencias = listOf(
            pendencia("1", "bruno", hoje, dataDaFalta = dataDaFalta),
            pendencia("2", "ana", hoje),
        )

        val resumo = calcularResumoDePendencias(listOf(ana, bruno), pendencias, hoje)

        assertEquals("Bruno", resumo.lembretes[0].nomeDoAluno)
        assertEquals(dataDaFalta, resumo.lembretes[0].dataDaFalta)
        assertNull(resumo.lembretes[1].dataDaFalta)
    }
}
