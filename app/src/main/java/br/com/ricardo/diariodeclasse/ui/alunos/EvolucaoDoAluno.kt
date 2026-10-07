package br.com.ricardo.diariodeclasse.ui.alunos

import br.com.ricardo.diariodeclasse.data.local.entity.NivelRegistradoDoAluno
import java.time.LocalDate

/** Um trecho da evolução: o aluno ficou neste nível a partir de [desde]. */
data class PassoDaEvolucao(
    val nomeDoNivel: String,
    val desde: LocalDate,
)

/** Evolução do aluno numa métrica. O primeiro passo é o nível atual. */
data class EvolucaoNaMetrica(
    val metricaId: String,
    val nomeDaMetrica: String,
    /** Do mais recente para o mais antigo. */
    val passos: List<PassoDaEvolucao>,
)

/**
 * Transforma os registros das sondagens em mudanças de nível: sondagens seguidas
 * no mesmo nível viram um passo só, datado da primeira delas
 * ("Silábico com valor sonoro desde 15/09").
 *
 * [registros] vêm agrupados por métrica (como a consulta do banco devolve);
 * sondagens com data depois de [hoje] são ignoradas.
 */
fun calcularEvolucaoDoAluno(registros: List<NivelRegistradoDoAluno>, hoje: LocalDate): List<EvolucaoNaMetrica> {
    val idsDasMetricas = mutableListOf<String>()
    for (registro in registros) {
        if (registro.metricaId !in idsDasMetricas) {
            idsDasMetricas.add(registro.metricaId)
        }
    }

    val evolucoes = mutableListOf<EvolucaoNaMetrica>()
    for (metricaId in idsDasMetricas) {
        val daMetrica: List<NivelRegistradoDoAluno> = registrosAteHoje(registros, metricaId, hoje)
        if (daMetrica.isEmpty()) {
            continue
        }
        evolucoes.add(
            EvolucaoNaMetrica(
                metricaId = metricaId,
                nomeDaMetrica = daMetrica.first().nomeDaMetrica,
                passos = juntarNiveisRepetidos(daMetrica),
            )
        )
    }
    return evolucoes
}

/** Os registros da métrica até hoje, do mais antigo para o mais recente. */
private fun registrosAteHoje(
    registros: List<NivelRegistradoDoAluno>,
    metricaId: String,
    hoje: LocalDate,
): List<NivelRegistradoDoAluno> {
    val selecionados = mutableListOf<NivelRegistradoDoAluno>()
    for (registro in registros) {
        if (registro.metricaId == metricaId && !registro.data.isAfter(hoje)) {
            selecionados.add(registro)
        }
    }
    return selecionados.sortedBy { registro -> registro.data }
}

/** Recebe do mais antigo para o mais recente e devolve os passos do mais recente para o mais antigo. */
private fun juntarNiveisRepetidos(registrosEmOrdem: List<NivelRegistradoDoAluno>): List<PassoDaEvolucao> {
    val passos = mutableListOf<PassoDaEvolucao>()
    var nivelAnteriorId: String? = null
    for (registro in registrosEmOrdem) {
        if (registro.nivelId != nivelAnteriorId) {
            passos.add(PassoDaEvolucao(nomeDoNivel = registro.nomeDoNivel, desde = registro.data))
            nivelAnteriorId = registro.nivelId
        }
    }
    return passos.reversed()
}
