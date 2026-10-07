package br.com.ricardo.diariodeclasse.ui.metas

import br.com.ricardo.diariodeclasse.data.local.entity.Aluno
import br.com.ricardo.diariodeclasse.data.local.entity.AlunoNaMeta
import br.com.ricardo.diariodeclasse.data.local.entity.Meta
import br.com.ricardo.diariodeclasse.data.local.entity.NivelDaMetrica
import br.com.ricardo.diariodeclasse.data.local.entity.ResultadoDatado
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

/** Cenário do exemplo real: silábicos com valor sonoro que devem virar alfabéticos. */
class ProgressoDaMetaTest {
    private val agora: Instant = Instant.parse("2026-10-06T11:00:00Z")
    private val hoje: LocalDate = LocalDate.of(2026, 10, 6)
    private val inicioDaMeta: LocalDate = LocalDate.of(2026, 9, 15)

    private val semValor = nivel("sem-valor", 0)
    private val comValor = nivel("com-valor", 1)
    private val silabicoAlfabetico = nivel("silabico-alfabetico", 2)
    private val alfabetico = nivel("alfabetico", 3)
    private val niveis = listOf(semValor, comValor, silabicoAlfabetico, alfabetico)

    private val ana = aluno("ana", "Ana")
    private val bruno = aluno("bruno", "Bruno")
    private val carla = aluno("carla", "Carla")
    private val davi = aluno("davi", "Davi")
    private val eva = aluno("eva", "Eva")
    private val turma = listOf(ana, bruno, carla, davi, eva)

    private val meta = Meta(
        id = "meta", turmaId = "turma", descricao = "Virar alfabéticos", metricaId = "escrita",
        nivelAlvoId = "alfabetico", prazo = LocalDate.of(2026, 11, 6), createdAt = agora, updatedAt = agora,
    )

    private fun nivel(id: String, ordem: Int): NivelDaMetrica {
        return NivelDaMetrica(id = id, metricaId = "escrita", nome = id, ordem = ordem, createdAt = agora, updatedAt = agora)
    }

    private fun aluno(id: String, nome: String): Aluno {
        return Aluno(id = id, turmaId = "turma", nome = nome, createdAt = agora, updatedAt = agora)
    }

    private fun naMeta(alunoId: String, nivelInicialId: String?): AlunoNaMeta {
        return AlunoNaMeta(
            id = "na-meta-$alunoId", metaId = "meta", alunoId = alunoId,
            nivelInicialId = nivelInicialId, createdAt = agora, updatedAt = agora,
        )
    }

    private fun resultado(alunoId: String, nivelId: String, data: LocalDate): ResultadoDatado {
        return ResultadoDatado(metricaId = "escrita", alunoId = alunoId, nivelId = nivelId, data = data)
    }

    private fun situacoes(progresso: ProgressoDaMeta): List<Pair<String, SituacaoNaMeta>> {
        return progresso.alunos.map { item -> Pair(item.aluno.id, item.situacao) }
    }

    @Test
    fun classificaCadaAlunoPeloNivelMaisRecente() {
        val alunosDaMeta = listOf(
            naMeta("ana", "com-valor"),
            naMeta("bruno", "com-valor"),
            naMeta("carla", "com-valor"),
            naMeta("davi", null),
        )
        val resultados = listOf(
            resultado("ana", "com-valor", inicioDaMeta),
            resultado("ana", "alfabetico", hoje),
            resultado("bruno", "com-valor", inicioDaMeta),
            resultado("bruno", "silabico-alfabetico", hoje),
            resultado("carla", "com-valor", inicioDaMeta),
        )

        val progresso = calcularProgressoDaMeta(meta, niveis, alunosDaMeta, turma, resultados, hoje)

        assertEquals(
            listOf(
                Pair("ana", SituacaoNaMeta.ATINGIU),
                Pair("bruno", SituacaoNaMeta.AVANCOU),
                Pair("carla", SituacaoNaMeta.NAO_AVANCOU),
                Pair("davi", SituacaoNaMeta.SEM_AVALIACAO),
            ),
            situacoes(progresso),
        )
        assertEquals(4, progresso.total())
        assertEquals(1, progresso.quantidadeNaSituacao(SituacaoNaMeta.ATINGIU))
    }

    @Test
    fun acimaDoAlvo_tambemContaComoAtingido() {
        val metaMaisBaixa = meta.copy(nivelAlvoId = "silabico-alfabetico")
        val resultados = listOf(resultado("ana", "alfabetico", hoje))

        val progresso = calcularProgressoDaMeta(metaMaisBaixa, niveis, listOf(naMeta("ana", "com-valor")), turma, resultados, hoje)

        assertEquals(SituacaoNaMeta.ATINGIU, progresso.alunos.single().situacao)
    }

    @Test
    fun alunoForaDaTurma_naoContaNoTotal() {
        val alunosDaMeta = listOf(naMeta("ana", "com-valor"), naMeta("excluido", "com-valor"))

        val progresso = calcularProgressoDaMeta(meta, niveis, alunosDaMeta, turma, emptyList(), hoje)

        assertEquals(1, progresso.total())
    }

    @Test
    fun resultadosDeOutraMetrica_naoContam() {
        val deOutraMetrica = ResultadoDatado(metricaId = "matematica", alunoId = "ana", nivelId = "alfabetico", data = hoje)

        val progresso = calcularProgressoDaMeta(meta, niveis, listOf(naMeta("ana", "com-valor")), turma, listOf(deOutraMetrica), hoje)

        assertEquals(SituacaoNaMeta.SEM_AVALIACAO, progresso.alunos.single().situacao)
    }

    @Test
    fun metaAMao_usaAMarcacaoDaProfessoraEIgnoraAsSondagens() {
        val metaAMao = meta.copy(metricaId = null, nivelAlvoId = null, prazo = null)
        val alunosDaMeta = listOf(
            naMeta("ana", null).copy(atingiuEm = hoje),
            naMeta("bruno", null),
        )
        // Bruno é alfabético na métrica de escrita, mas esta meta não usa métrica.
        val resultados = listOf(resultado("bruno", "alfabetico", hoje))

        val progresso = calcularProgressoDaMeta(metaAMao, niveis, alunosDaMeta, turma, resultados, hoje)

        assertEquals(
            listOf(Pair("ana", SituacaoNaMeta.ATINGIU), Pair("bruno", SituacaoNaMeta.AINDA_NAO)),
            situacoes(progresso),
        )
        assertEquals(null, progresso.nivelAlvo)
    }

    @Test
    fun metaAMao_mantemOrdemAlfabeticaSemAgrupar() {
        val metaAMao = meta.copy(metricaId = null, nivelAlvoId = null)
        val alunosDaMeta = listOf(
            naMeta("ana", null),
            naMeta("bruno", null).copy(atingiuEm = hoje),
            naMeta("carla", null),
        )

        val progresso = calcularProgressoDaMeta(metaAMao, emptyList(), alunosDaMeta, turma, emptyList(), hoje)

        assertEquals(listOf("ana", "bruno", "carla"), progresso.alunos.map { item -> item.aluno.id })
        assertEquals(1, progresso.quantidadeNaSituacao(SituacaoNaMeta.ATINGIU))
    }

    @Test
    fun agrupaPorSituacaoMantendoOrdemAlfabetica() {
        val alunosDaMeta = listOf(naMeta("eva", "com-valor"), naMeta("ana", "com-valor"), naMeta("carla", "com-valor"))
        val resultados = listOf(
            resultado("eva", "alfabetico", hoje),
            resultado("carla", "alfabetico", hoje),
            resultado("ana", "com-valor", hoje),
        )

        val progresso = calcularProgressoDaMeta(meta, niveis, alunosDaMeta, turma, resultados, hoje)

        assertEquals(listOf("carla", "eva", "ana"), progresso.alunos.map { item -> item.aluno.id })
    }
}
