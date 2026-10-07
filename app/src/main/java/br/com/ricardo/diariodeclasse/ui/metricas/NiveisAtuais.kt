package br.com.ricardo.diariodeclasse.ui.metricas

import br.com.ricardo.diariodeclasse.data.local.entity.Aluno
import br.com.ricardo.diariodeclasse.data.local.entity.NivelDaMetrica
import br.com.ricardo.diariodeclasse.data.local.entity.ResultadoDatado
import java.time.LocalDate

/** Quantos alunos estão hoje num nível da escala. */
data class FaixaDaDistribuicao(
    val nivel: NivelDaMetrica,
    val quantidade: Int,
)

/** Retrato da turma numa métrica: "14 alfabéticos, 11 silábicos com valor sonoro...". */
data class DistribuicaoDaMetrica(
    /** Uma faixa por nível, na ordem da escala, inclusive os níveis sem ninguém. */
    val faixas: List<FaixaDaDistribuicao>,
    /** Alunos da turma que ainda não foram avaliados nesta métrica. */
    val semAvaliacao: Int,
)

/**
 * O resultado mais recente de cada aluno com data até [ate] (inclusive), indexado
 * pelo id do aluno. Vale a data da sondagem, não a hora do registro: uma sondagem
 * de setembro lançada em outubro entra no lugar certo da história.
 *
 * Os [resultados] devem ser de uma métrica só.
 */
fun nivelAtualDeCadaAluno(resultados: List<ResultadoDatado>, ate: LocalDate): Map<String, ResultadoDatado> {
    val maisRecentes = mutableMapOf<String, ResultadoDatado>()
    for (resultado in resultados) {
        if (resultado.data.isAfter(ate)) {
            continue
        }
        val guardado: ResultadoDatado? = maisRecentes[resultado.alunoId]
        if (guardado == null || resultado.data.isAfter(guardado.data)) {
            maisRecentes[resultado.alunoId] = resultado
        }
    }
    return maisRecentes
}

/** Só os resultados da métrica [metricaId], para quem tem a lista da turma inteira. */
fun resultadosDaMetrica(resultados: List<ResultadoDatado>, metricaId: String): List<ResultadoDatado> {
    val daMetrica = mutableListOf<ResultadoDatado>()
    for (resultado in resultados) {
        if (resultado.metricaId == metricaId) {
            daMetrica.add(resultado)
        }
    }
    return daMetrica
}

/** Só os níveis da métrica [metricaId], mantendo a ordem da escala. */
fun niveisDaMetrica(niveis: List<NivelDaMetrica>, metricaId: String): List<NivelDaMetrica> {
    val daMetrica = mutableListOf<NivelDaMetrica>()
    for (nivel in niveis) {
        if (nivel.metricaId == metricaId) {
            daMetrica.add(nivel)
        }
    }
    return daMetrica
}

/**
 * Conta só os [alunos] que estão na turma agora: aluno excluído continua com
 * resultados no banco, mas não entra no retrato da turma.
 */
fun calcularDistribuicao(
    niveis: List<NivelDaMetrica>,
    alunos: List<Aluno>,
    nivelAtual: Map<String, ResultadoDatado>,
): DistribuicaoDaMetrica {
    val faixas = mutableListOf<FaixaDaDistribuicao>()
    for (nivel in niveis) {
        var quantidade = 0
        for (aluno in alunos) {
            val resultado: ResultadoDatado? = nivelAtual[aluno.id]
            if (resultado != null && resultado.nivelId == nivel.id) {
                quantidade = quantidade + 1
            }
        }
        faixas.add(FaixaDaDistribuicao(nivel, quantidade))
    }

    var semAvaliacao = 0
    for (aluno in alunos) {
        if (nivelAtual[aluno.id] == null) {
            semAvaliacao = semAvaliacao + 1
        }
    }

    return DistribuicaoDaMetrica(faixas = faixas, semAvaliacao = semAvaliacao)
}

fun buscarNivel(niveis: List<NivelDaMetrica>, nivelId: String?): NivelDaMetrica? {
    if (nivelId == null) {
        return null
    }
    for (nivel in niveis) {
        if (nivel.id == nivelId) {
            return nivel
        }
    }
    return null
}
