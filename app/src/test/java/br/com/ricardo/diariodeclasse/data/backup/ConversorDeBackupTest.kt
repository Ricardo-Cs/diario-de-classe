package br.com.ricardo.diariodeclasse.data.backup

import br.com.ricardo.diariodeclasse.data.local.entity.Aluno
import br.com.ricardo.diariodeclasse.data.local.entity.Anotacao
import br.com.ricardo.diariodeclasse.data.local.entity.Chamada
import br.com.ricardo.diariodeclasse.data.local.entity.DadosDoDiario
import br.com.ricardo.diariodeclasse.data.local.entity.Pendencia
import br.com.ricardo.diariodeclasse.data.local.entity.Periodo
import br.com.ricardo.diariodeclasse.data.local.entity.RegistroPresenca
import br.com.ricardo.diariodeclasse.data.local.entity.StatusPendencia
import br.com.ricardo.diariodeclasse.data.local.entity.Turma
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class ConversorDeBackupTest {
    private val agora: Instant = Instant.parse("2026-10-06T11:00:00Z")
    private val hoje: LocalDate = LocalDate.of(2026, 10, 6)

    /** Um diário pequeno com um item de cada tipo, incluindo campos opcionais preenchidos. */
    private fun diarioDeExemplo(): DadosDoDiario {
        val turma = Turma(
            id = "turma", nome = "1º ano A", anoSerie = "1º ano", periodo = Periodo.MANHA,
            anoLetivo = 2026, createdAt = agora, updatedAt = agora,
        )
        val ana = Aluno(id = "ana", turmaId = "turma", nome = "Ana", createdAt = agora, updatedAt = agora)
        val brunoExcluido = Aluno(
            id = "bruno", turmaId = "turma", nome = "Bruno",
            createdAt = agora, updatedAt = agora, deletedAt = agora,
        )
        val chamada = Chamada(id = "chamada", turmaId = "turma", data = hoje, createdAt = agora, updatedAt = agora)
        val falta = RegistroPresenca(
            id = "falta", chamadaId = "chamada", alunoId = "ana", presente = false,
            observacao = "Atestado", createdAt = agora, updatedAt = agora,
        )
        val pendencia = Pendencia(
            id = "pendencia", alunoId = "ana", descricao = "Ficha de português",
            dataLembrete = hoje.plusDays(1), status = StatusPendencia.ENTREGUE,
            registroPresencaId = "falta", entregueEm = agora, createdAt = agora, updatedAt = agora,
        )
        val anotacao = Anotacao(
            id = "anotacao", alunoId = "ana", texto = "Reconheceu as vogais",
            data = hoje, createdAt = agora, updatedAt = agora,
        )
        return DadosDoDiario(
            turmas = listOf(turma),
            alunos = listOf(ana, brunoExcluido),
            chamadas = listOf(chamada),
            registrosPresenca = listOf(falta),
            pendencias = listOf(pendencia),
            anotacoes = listOf(anotacao),
        )
    }

    @Test
    fun exportarELer_devolveExatamenteOsMesmosDados() {
        val original: DadosDoDiario = diarioDeExemplo()

        val json: String = ConversorDeBackup.gerarJson(original, agora)
        val leitura: LeituraDoBackup = ConversorDeBackup.lerJson(json)

        assertTrue(leitura is LeituraDoBackup.Valida)
        val valida = leitura as LeituraDoBackup.Valida
        assertEquals(original, valida.dados)
        assertEquals(agora, valida.exportadoEm)
    }

    @Test
    fun textoQueNaoEJson_naoEBackupDoApp() {
        val leitura: LeituraDoBackup = ConversorDeBackup.lerJson("lista de compras: arroz, feijão")

        assertEquals(LeituraDoBackup.Invalida(MotivoDeArquivoInvalido.NAO_E_BACKUP_DO_APP), leitura)
    }

    @Test
    fun jsonDeOutroFormato_naoEBackupDoApp() {
        val leitura: LeituraDoBackup = ConversorDeBackup.lerJson("""{"formato": "outro-app", "versao": 1}""")

        assertEquals(LeituraDoBackup.Invalida(MotivoDeArquivoInvalido.NAO_E_BACKUP_DO_APP), leitura)
    }

    @Test
    fun versaoMaisNova_avisaQueOAppPrecisaSerAtualizado() {
        val json = """{"formato": "$FORMATO_DO_ARQUIVO", "versao": ${VERSAO_DO_FORMATO + 1}, "campoNovo": true}"""

        val leitura: LeituraDoBackup = ConversorDeBackup.lerJson(json)

        assertEquals(LeituraDoBackup.Invalida(MotivoDeArquivoInvalido.VERSAO_MAIS_NOVA), leitura)
    }

    @Test
    fun arquivoDoAppIncompleto_estaDanificado() {
        val json = """{"formato": "$FORMATO_DO_ARQUIVO", "versao": $VERSAO_DO_FORMATO}"""

        val leitura: LeituraDoBackup = ConversorDeBackup.lerJson(json)

        assertEquals(LeituraDoBackup.Invalida(MotivoDeArquivoInvalido.ARQUIVO_DANIFICADO), leitura)
    }

    @Test
    fun dataImpossivel_estaDanificado() {
        val json: String = ConversorDeBackup.gerarJson(diarioDeExemplo(), agora)
            .replace("\"2026-10-06\"", "\"2026-13-45\"")

        val leitura: LeituraDoBackup = ConversorDeBackup.lerJson(json)

        assertEquals(LeituraDoBackup.Invalida(MotivoDeArquivoInvalido.ARQUIVO_DANIFICADO), leitura)
    }

    @Test
    fun periodoDesconhecido_estaDanificado() {
        val json: String = ConversorDeBackup.gerarJson(diarioDeExemplo(), agora)
            .replace("\"MANHA\"", "\"MADRUGADA\"")

        val leitura: LeituraDoBackup = ConversorDeBackup.lerJson(json)

        assertEquals(LeituraDoBackup.Invalida(MotivoDeArquivoInvalido.ARQUIVO_DANIFICADO), leitura)
    }

    @Test
    fun resumo_contaSoOQueApareceNoApp() {
        val resumo: ResumoDoBackup = resumirBackup(diarioDeExemplo())

        // Bruno está excluído; a única pendência já foi entregue.
        assertEquals(ResumoDoBackup(turmas = 1, alunos = 1, chamadas = 1, pendenciasEmAberto = 0, anotacoes = 1), resumo)
    }

    @Test
    fun resumo_alunosDeTurmaExcluidaNaoContam() {
        val dados: DadosDoDiario = diarioDeExemplo()
        val turmaExcluida: Turma = dados.turmas.first().copy(deletedAt = agora)
        val semTurma: DadosDoDiario = dados.copy(turmas = listOf(turmaExcluida))

        val resumo: ResumoDoBackup = resumirBackup(semTurma)

        assertEquals(ResumoDoBackup(turmas = 0, alunos = 0, chamadas = 0, pendenciasEmAberto = 0, anotacoes = 0), resumo)
    }
}
