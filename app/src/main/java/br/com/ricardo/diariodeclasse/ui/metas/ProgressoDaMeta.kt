package br.com.ricardo.diariodeclasse.ui.metas

import br.com.ricardo.diariodeclasse.data.local.entity.Aluno
import br.com.ricardo.diariodeclasse.data.local.entity.AlunoNaMeta
import br.com.ricardo.diariodeclasse.data.local.entity.Meta
import br.com.ricardo.diariodeclasse.data.local.entity.NivelDaMetrica
import br.com.ricardo.diariodeclasse.data.local.entity.ResultadoDatado
import br.com.ricardo.diariodeclasse.ui.metricas.buscarNivel
import br.com.ricardo.diariodeclasse.ui.metricas.nivelAtualDeCadaAluno
import br.com.ricardo.diariodeclasse.ui.metricas.resultadosDaMetrica
import java.time.LocalDate

/** A ordem das constantes é a ordem em que os grupos aparecem na tela da meta. */
enum class SituacaoNaMeta {
    /** Está no nível-alvo ou acima. */
    ATINGIU,

    /** Subiu em relação ao nível em que entrou na meta, mas ainda não chegou ao alvo. */
    AVANCOU,

    /** Avaliado, abaixo do alvo e sem avanço desde que entrou na meta (ou sem nível inicial para comparar). */
    NAO_AVANCOU,

    /** Ainda não tem nenhum resultado nesta métrica. */
    SEM_AVALIACAO,
}

data class AlunoNoProgresso(
    val aluno: Aluno,
    val situacao: SituacaoNaMeta,
    val nivelAtual: NivelDaMetrica?,
)

data class ProgressoDaMeta(
    val nivelAlvo: NivelDaMetrica?,
    /** Ordenados por situação e, dentro de cada uma, pelo nome. */
    val alunos: List<AlunoNoProgresso>,
) {
    fun total(): Int {
        return alunos.size
    }

    fun quantidadeNaSituacao(situacao: SituacaoNaMeta): Int {
        var quantidade = 0
        for (aluno in alunos) {
            if (aluno.situacao == situacao) {
                quantidade = quantidade + 1
            }
        }
        return quantidade
    }

    fun alunosNaSituacao(situacao: SituacaoNaMeta): List<AlunoNoProgresso> {
        val naSituacao = mutableListOf<AlunoNoProgresso>()
        for (aluno in alunos) {
            if (aluno.situacao == situacao) {
                naSituacao.add(aluno)
            }
        }
        return naSituacao
    }
}

/**
 * Situação de cada aluno da meta a partir das sondagens até [hoje].
 *
 * [alunosDaTurma] são os alunos visíveis, em ordem alfabética: quem foi excluído
 * da turma deixa de contar no total da meta.
 * [resultados] pode trazer todas as métricas da turma; os da meta são filtrados aqui.
 */
fun calcularProgressoDaMeta(
    meta: Meta,
    niveis: List<NivelDaMetrica>,
    alunosDaMeta: List<AlunoNaMeta>,
    alunosDaTurma: List<Aluno>,
    resultados: List<ResultadoDatado>,
    hoje: LocalDate,
): ProgressoDaMeta {
    val nivelAlvo: NivelDaMetrica? = buscarNivel(niveis, meta.nivelAlvoId)
    val nivelAtual: Map<String, ResultadoDatado> = nivelAtualDeCadaAluno(
        resultadosDaMetrica(resultados, meta.metricaId),
        hoje,
    )

    val calculados = mutableListOf<AlunoNoProgresso>()
    for (aluno in alunosDaTurma) {
        val naMeta: AlunoNaMeta = buscarAlunoNaMeta(alunosDaMeta, aluno.id) ?: continue
        val resultadoAtual: ResultadoDatado? = nivelAtual[aluno.id]
        var atual: NivelDaMetrica? = null
        if (resultadoAtual != null) {
            atual = buscarNivel(niveis, resultadoAtual.nivelId)
        }
        val inicial: NivelDaMetrica? = buscarNivel(niveis, naMeta.nivelInicialId)
        val situacao: SituacaoNaMeta = definirSituacao(atual, inicial, nivelAlvo)
        calculados.add(AlunoNoProgresso(aluno, situacao, atual))
    }

    // Agrupa pela ordem do enum mantendo a ordem alfabética dentro de cada grupo.
    val ordenados = mutableListOf<AlunoNoProgresso>()
    for (situacao in SituacaoNaMeta.entries) {
        for (calculado in calculados) {
            if (calculado.situacao == situacao) {
                ordenados.add(calculado)
            }
        }
    }

    return ProgressoDaMeta(nivelAlvo = nivelAlvo, alunos = ordenados)
}

private fun definirSituacao(
    atual: NivelDaMetrica?,
    inicial: NivelDaMetrica?,
    alvo: NivelDaMetrica?,
): SituacaoNaMeta {
    if (atual == null) {
        return SituacaoNaMeta.SEM_AVALIACAO
    }
    if (alvo != null && atual.ordem >= alvo.ordem) {
        return SituacaoNaMeta.ATINGIU
    }
    if (inicial != null && atual.ordem > inicial.ordem) {
        return SituacaoNaMeta.AVANCOU
    }
    return SituacaoNaMeta.NAO_AVANCOU
}

private fun buscarAlunoNaMeta(alunosDaMeta: List<AlunoNaMeta>, alunoId: String): AlunoNaMeta? {
    for (naMeta in alunosDaMeta) {
        if (naMeta.alunoId == alunoId) {
            return naMeta
        }
    }
    return null
}
