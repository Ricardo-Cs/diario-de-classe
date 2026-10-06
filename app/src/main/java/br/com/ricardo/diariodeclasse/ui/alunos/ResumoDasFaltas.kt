package br.com.ricardo.diariodeclasse.ui.alunos

import br.com.ricardo.diariodeclasse.data.local.entity.FaltaDoAluno
import java.time.LocalDate
import java.time.YearMonth

/** O que a seção de faltas da tela do aluno mostra. */
data class ResumoDasFaltas(
    /** Faltas no mês corrente: o número que a professora costuma acompanhar. */
    val noMes: Int,
    /** As mais recentes primeiro, limitadas a [MAXIMO_DE_FALTAS_NA_TELA_DO_ALUNO]. */
    val recentes: List<FaltaDoAluno>,
    /** Faltas mais antigas que ficaram de fora da lista. */
    val naoMostradas: Int,
)

/** A seção é para conferência rápida; o histórico completo fica para uma tela futura. */
const val MAXIMO_DE_FALTAS_NA_TELA_DO_ALUNO = 5

/** [faltas] chegam do banco da mais recente para a mais antiga. */
fun calcularResumoDasFaltas(faltas: List<FaltaDoAluno>, hoje: LocalDate): ResumoDasFaltas {
    val mesAtual: YearMonth = YearMonth.from(hoje)
    var noMes = 0
    val recentes = mutableListOf<FaltaDoAluno>()

    for (falta in faltas) {
        if (YearMonth.from(falta.data) == mesAtual) {
            noMes = noMes + 1
        }
        if (recentes.size < MAXIMO_DE_FALTAS_NA_TELA_DO_ALUNO) {
            recentes.add(falta)
        }
    }

    return ResumoDasFaltas(
        noMes = noMes,
        recentes = recentes,
        naoMostradas = faltas.size - recentes.size,
    )
}
