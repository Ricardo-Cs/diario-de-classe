package br.com.ricardo.diariodeclasse.ui.inicio

import br.com.ricardo.diariodeclasse.data.local.entity.Aluno
import br.com.ricardo.diariodeclasse.data.local.entity.PendenciaComOrigem
import java.time.LocalDate

/** Uma linha da área de lembretes do Início: quem, o quê e de onde veio. */
data class LembreteDePendencia(
    val pendenciaId: String,
    val nomeDoAluno: String,
    val descricao: String,
    /** `null` = pendência avulsa. */
    val dataDaFalta: LocalDate?,
)

data class ResumoDePendencias(
    /** Pendentes com lembrete para hoje ou já vencido. */
    val paraHoje: Int,
    /** Todas as não entregues, inclusive as com lembrete futuro. */
    val emAberto: Int,
    /** Só as primeiras de hoje; o resto fica na tela de pendências. */
    val lembretes: List<LembreteDePendencia>,
)

/** O Início mostra poucos lembretes para não virar uma lista longa. */
const val MAXIMO_DE_LEMBRETES_NO_INICIO = 3

/**
 * As pendências chegam do banco ordenadas pelo lembrete (mais antigo primeiro),
 * então os lembretes mostrados são os que estão esperando há mais tempo.
 */
fun calcularResumoDePendencias(
    alunos: List<Aluno>,
    pendencias: List<PendenciaComOrigem>,
    hoje: LocalDate,
): ResumoDePendencias {
    var paraHoje = 0
    val lembretes = mutableListOf<LembreteDePendencia>()

    for (item in pendencias) {
        if (!item.pendencia.estaPendenteEm(hoje)) {
            continue
        }
        paraHoje = paraHoje + 1

        val aluno: Aluno? = buscarAluno(alunos, item.pendencia.alunoId)
        if (aluno != null && lembretes.size < MAXIMO_DE_LEMBRETES_NO_INICIO) {
            lembretes.add(
                LembreteDePendencia(
                    pendenciaId = item.pendencia.id,
                    nomeDoAluno = aluno.nome,
                    descricao = item.pendencia.descricao,
                    dataDaFalta = item.dataDaFalta,
                )
            )
        }
    }

    return ResumoDePendencias(
        paraHoje = paraHoje,
        emAberto = pendencias.size,
        lembretes = lembretes,
    )
}

private fun buscarAluno(alunos: List<Aluno>, alunoId: String): Aluno? {
    for (aluno in alunos) {
        if (aluno.id == alunoId) {
            return aluno
        }
    }
    return null
}
