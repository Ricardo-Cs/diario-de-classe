package br.com.ricardo.diariodeclasse.ui.inicio

import br.com.ricardo.diariodeclasse.data.local.entity.Aluno
import br.com.ricardo.diariodeclasse.data.local.entity.Chamada
import br.com.ricardo.diariodeclasse.data.local.entity.RegistroPresenca
import br.com.ricardo.diariodeclasse.data.repository.ChamadaDoDia
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class SituacaoDaChamadaTest {
    private val fusoDeSaoPaulo: ZoneId = ZoneId.of("America/Sao_Paulo")

    // 10:42 em UTC = 07:42 em São Paulo
    private val momentoDaChamada: Instant = Instant.parse("2026-10-06T10:42:00Z")

    private val ana = alunoDeTeste("ana")
    private val bruno = alunoDeTeste("bruno")
    private val carla = alunoDeTeste("carla")

    private fun alunoDeTeste(id: String): Aluno {
        return Aluno(
            id = id,
            turmaId = "turma",
            nome = id,
            createdAt = momentoDaChamada,
            updatedAt = momentoDaChamada,
        )
    }

    private fun registro(alunoId: String, presente: Boolean): RegistroPresenca {
        return RegistroPresenca(
            id = "registro-$alunoId",
            chamadaId = "chamada",
            alunoId = alunoId,
            presente = presente,
            observacao = null,
            createdAt = momentoDaChamada,
            updatedAt = momentoDaChamada,
        )
    }

    private fun chamadaCom(registros: List<RegistroPresenca>): ChamadaDoDia {
        val chamada = Chamada(
            id = "chamada",
            turmaId = "turma",
            data = LocalDate.of(2026, 10, 6),
            createdAt = momentoDaChamada,
            updatedAt = momentoDaChamada,
        )
        return ChamadaDoDia(chamada, registros)
    }

    @Test
    fun turmaSemAlunos() {
        val situacao = calcularSituacaoDaChamada(emptyList(), null, fusoDeSaoPaulo)

        assertEquals(SituacaoDaChamada.TurmaSemAlunos, situacao)
    }

    @Test
    fun semChamada_naoFeitaComTotalDeAlunos() {
        val situacao = calcularSituacaoDaChamada(listOf(ana, bruno), null, fusoDeSaoPaulo)

        assertEquals(SituacaoDaChamada.NaoFeita(totalDeAlunos = 2), situacao)
    }

    @Test
    fun comChamada_contaPresentesEAusentesNoHorarioLocal() {
        val chamada = chamadaCom(listOf(registro("ana", true), registro("bruno", false)))

        val situacao = calcularSituacaoDaChamada(listOf(ana, bruno), chamada, fusoDeSaoPaulo)

        val esperado = SituacaoDaChamada.Feita(horario = LocalTime.of(7, 42), presentes = 1, ausentes = 1)
        assertEquals(esperado, situacao)
    }

    @Test
    fun alunoCadastradoDepoisDaChamada_contaComoPresente() {
        val chamada = chamadaCom(listOf(registro("ana", true), registro("bruno", false)))

        val situacao = calcularSituacaoDaChamada(listOf(ana, bruno, carla), chamada, fusoDeSaoPaulo)

        val esperado = SituacaoDaChamada.Feita(horario = LocalTime.of(7, 42), presentes = 2, ausentes = 1)
        assertEquals(esperado, situacao)
    }

    @Test
    fun alunoExcluidoDepoisDaChamada_naoContaComoFalta() {
        val chamada = chamadaCom(listOf(registro("ana", true), registro("bruno", false)))

        val situacao = calcularSituacaoDaChamada(listOf(ana), chamada, fusoDeSaoPaulo)

        val esperado = SituacaoDaChamada.Feita(horario = LocalTime.of(7, 42), presentes = 1, ausentes = 0)
        assertEquals(esperado, situacao)
    }
}
