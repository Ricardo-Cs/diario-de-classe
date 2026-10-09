package br.com.ricardo.diariodeclasse.data.backup

import br.com.ricardo.diariodeclasse.data.local.entity.Aluno
import br.com.ricardo.diariodeclasse.data.local.entity.AlunoNaMeta
import br.com.ricardo.diariodeclasse.data.local.entity.Anotacao
import br.com.ricardo.diariodeclasse.data.local.entity.Chamada
import br.com.ricardo.diariodeclasse.data.local.entity.DadosDoDiario
import br.com.ricardo.diariodeclasse.data.local.entity.Foto
import br.com.ricardo.diariodeclasse.data.local.entity.Lembrete
import br.com.ricardo.diariodeclasse.data.local.entity.Meta
import br.com.ricardo.diariodeclasse.data.local.entity.Metrica
import br.com.ricardo.diariodeclasse.data.local.entity.NivelDaMetrica
import br.com.ricardo.diariodeclasse.data.local.entity.Pendencia
import br.com.ricardo.diariodeclasse.data.local.entity.Periodo
import br.com.ricardo.diariodeclasse.data.local.entity.RegistroPresenca
import br.com.ricardo.diariodeclasse.data.local.entity.ResultadoDaSondagem
import br.com.ricardo.diariodeclasse.data.local.entity.Sondagem
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
        val metrica = Metrica(id = "escrita", turmaId = "turma", nome = "Nível de escrita", createdAt = agora, updatedAt = agora)
        val silabico = NivelDaMetrica(
            id = "silabico", metricaId = "escrita", nome = "Silábico", ordem = 0,
            createdAt = agora, updatedAt = agora,
        )
        val alfabetico = NivelDaMetrica(
            id = "alfabetico", metricaId = "escrita", nome = "Alfabético", ordem = 1,
            createdAt = agora, updatedAt = agora,
        )
        val sondagem = Sondagem(id = "sondagem", metricaId = "escrita", data = hoje, createdAt = agora, updatedAt = agora)
        val resultado = ResultadoDaSondagem(
            id = "resultado", sondagemId = "sondagem", alunoId = "ana", nivelId = "silabico",
            createdAt = agora, updatedAt = agora,
        )
        val meta = Meta(
            id = "meta", turmaId = "turma", descricao = "Virar alfabéticos", metricaId = "escrita",
            nivelAlvoId = "alfabetico", prazo = hoje.plusDays(31), encerradaEm = hoje,
            createdAt = agora, updatedAt = agora,
        )
        val anaNaMeta = AlunoNaMeta(
            id = "ana-na-meta", metaId = "meta", alunoId = "ana", nivelInicialId = "silabico",
            createdAt = agora, updatedAt = agora,
        )
        // Meta marcada à mão: sem métrica, sem prazo e com a Ana marcada como "atingiu".
        val metaAMao = Meta(
            id = "meta-a-mao", turmaId = "turma", descricao = "Família numérica do 10 ao 80",
            metricaId = null, nivelAlvoId = null, prazo = null, createdAt = agora, updatedAt = agora,
        )
        val anaNaMetaAMao = AlunoNaMeta(
            id = "ana-na-meta-a-mao", metaId = "meta-a-mao", alunoId = "ana", nivelInicialId = null,
            atingiuEm = hoje, createdAt = agora, updatedAt = agora,
        )
        return DadosDoDiario(
            turmas = listOf(turma),
            alunos = listOf(ana, brunoExcluido),
            chamadas = listOf(chamada),
            registrosPresenca = listOf(falta),
            pendencias = listOf(pendencia),
            anotacoes = listOf(anotacao),
            metricas = listOf(metrica),
            niveisDaMetrica = listOf(silabico, alfabetico),
            sondagens = listOf(sondagem),
            resultadosDaSondagem = listOf(resultado),
            metas = listOf(meta, metaAMao),
            alunosNaMeta = listOf(anaNaMeta, anaNaMetaAMao),
            lembretes = listOf(
                Lembrete(
                    id = "lembrete", descricao = "Entregar portfólio", data = hoje.plusDays(7),
                    concluidoEm = agora, createdAt = agora, updatedAt = agora,
                ),
            ),
            fotos = listOf(
                Foto(
                    id = "foto", turmaId = "turma", data = hoje, nomeDoArquivo = "foto.jpg",
                    legenda = "Pintura com guache", createdAt = agora, updatedAt = agora,
                ),
                Foto(
                    id = "foto-excluida", turmaId = "turma", data = hoje, nomeDoArquivo = "foto-excluida.jpg",
                    createdAt = agora, updatedAt = agora, deletedAt = agora,
                ),
            ),
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

    /** Arquivo exportado antes das métricas: sem os campos novos, que viram listas vazias. */
    @Test
    fun arquivoDaVersao1_continuaSendoLido() {
        val json = """
            {
              "formato": "$FORMATO_DO_ARQUIVO",
              "versao": 1,
              "exportadoEm": "2026-10-01T10:00:00Z",
              "turmas": [{"id": "turma", "nome": "1º ano A", "anoSerie": "1º ano", "periodo": "MANHA",
                          "anoLetivo": 2026, "createdAt": "2026-10-01T10:00:00Z", "updatedAt": "2026-10-01T10:00:00Z"}],
              "alunos": [],
              "chamadas": [],
              "registrosPresenca": [],
              "pendencias": [],
              "anotacoes": []
            }
        """.trimIndent()

        val leitura: LeituraDoBackup = ConversorDeBackup.lerJson(json)

        assertTrue(leitura is LeituraDoBackup.Valida)
        val dados: DadosDoDiario = (leitura as LeituraDoBackup.Valida).dados
        assertEquals(1, dados.turmas.size)
        assertEquals(emptyList<Metrica>(), dados.metricas)
        assertEquals(emptyList<Meta>(), dados.metas)
        assertEquals(emptyList<Foto>(), dados.fotos)
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

        // Bruno e uma das fotos estão excluídos; a única pendência já foi entregue.
        assertEquals(
            ResumoDoBackup(
                turmas = 1, alunos = 1, chamadas = 1, pendenciasEmAberto = 0, anotacoes = 1, sondagens = 1, fotos = 1,
            ),
            resumo,
        )
    }

    @Test
    fun resumo_sondagensDeMetricaExcluidaNaoContam() {
        val dados: DadosDoDiario = diarioDeExemplo()
        val metricaExcluida: Metrica = dados.metricas.first().copy(deletedAt = agora)

        val resumo: ResumoDoBackup = resumirBackup(dados.copy(metricas = listOf(metricaExcluida)))

        assertEquals(0, resumo.sondagens)
    }

    @Test
    fun resumo_alunosDeTurmaExcluidaNaoContam() {
        val dados: DadosDoDiario = diarioDeExemplo()
        val turmaExcluida: Turma = dados.turmas.first().copy(deletedAt = agora)
        val semTurma: DadosDoDiario = dados.copy(turmas = listOf(turmaExcluida))

        val resumo: ResumoDoBackup = resumirBackup(semTurma)

        assertEquals(
            ResumoDoBackup(
                turmas = 0, alunos = 0, chamadas = 0, pendenciasEmAberto = 0, anotacoes = 0, sondagens = 0, fotos = 0,
            ),
            resumo,
        )
    }
}
