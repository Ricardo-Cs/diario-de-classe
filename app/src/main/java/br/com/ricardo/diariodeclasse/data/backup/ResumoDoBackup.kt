package br.com.ricardo.diariodeclasse.data.backup

import br.com.ricardo.diariodeclasse.data.local.entity.Aluno
import br.com.ricardo.diariodeclasse.data.local.entity.Anotacao
import br.com.ricardo.diariodeclasse.data.local.entity.Chamada
import br.com.ricardo.diariodeclasse.data.local.entity.DadosDoDiario
import br.com.ricardo.diariodeclasse.data.local.entity.Foto
import br.com.ricardo.diariodeclasse.data.local.entity.Metrica
import br.com.ricardo.diariodeclasse.data.local.entity.Pendencia
import br.com.ricardo.diariodeclasse.data.local.entity.Sondagem
import br.com.ricardo.diariodeclasse.data.local.entity.StatusPendencia
import br.com.ricardo.diariodeclasse.data.local.entity.Turma

/**
 * O que a professora vê antes de confirmar a importação ("3 turmas, 74 alunos...").
 * Conta só o que aparece no app. Itens excluídos vão no arquivo, mas não entram aqui.
 */
data class ResumoDoBackup(
    val turmas: Int,
    val alunos: Int,
    val chamadas: Int,
    /** Só as ainda não entregues. */
    val pendenciasEmAberto: Int,
    val anotacoes: Int,
    val sondagens: Int,
    val fotos: Int,
)

/**
 * Segue a mesma regra das telas: excluir uma turma não marca os alunos dela como
 * excluídos, eles só deixam de aparecer porque a turma some. Por isso um aluno
 * conta só se ele e a turma dele estiverem visíveis, e assim por diante.
 */
fun resumirBackup(dados: DadosDoDiario): ResumoDoBackup {
    val turmasVisiveis: Set<String> = idsDasTurmasVisiveis(dados.turmas)
    val alunosVisiveis: Set<String> = idsDosAlunosVisiveis(dados.alunos, turmasVisiveis)

    return ResumoDoBackup(
        turmas = turmasVisiveis.size,
        alunos = alunosVisiveis.size,
        chamadas = contarChamadasVisiveis(dados.chamadas, turmasVisiveis),
        pendenciasEmAberto = contarPendenciasEmAberto(dados.pendencias, alunosVisiveis),
        anotacoes = contarAnotacoesVisiveis(dados.anotacoes, alunosVisiveis),
        sondagens = contarSondagensVisiveis(dados.sondagens, idsDasMetricasVisiveis(dados.metricas, turmasVisiveis)),
        fotos = contarFotosVisiveis(dados.fotos, turmasVisiveis),
    )
}

private fun contarFotosVisiveis(fotos: List<Foto>, turmasVisiveis: Set<String>): Int {
    var quantidade = 0
    for (foto in fotos) {
        if (foto.deletedAt == null && foto.turmaId in turmasVisiveis) {
            quantidade = quantidade + 1
        }
    }
    return quantidade
}

private fun idsDasMetricasVisiveis(metricas: List<Metrica>, turmasVisiveis: Set<String>): Set<String> {
    val ids = mutableSetOf<String>()
    for (metrica in metricas) {
        if (metrica.deletedAt == null && metrica.turmaId in turmasVisiveis) {
            ids.add(metrica.id)
        }
    }
    return ids
}

private fun contarSondagensVisiveis(sondagens: List<Sondagem>, metricasVisiveis: Set<String>): Int {
    var quantidade = 0
    for (sondagem in sondagens) {
        if (sondagem.deletedAt == null && sondagem.metricaId in metricasVisiveis) {
            quantidade = quantidade + 1
        }
    }
    return quantidade
}

private fun idsDasTurmasVisiveis(turmas: List<Turma>): Set<String> {
    val ids = mutableSetOf<String>()
    for (turma in turmas) {
        if (turma.deletedAt == null) {
            ids.add(turma.id)
        }
    }
    return ids
}

private fun idsDosAlunosVisiveis(alunos: List<Aluno>, turmasVisiveis: Set<String>): Set<String> {
    val ids = mutableSetOf<String>()
    for (aluno in alunos) {
        if (aluno.deletedAt == null && aluno.turmaId in turmasVisiveis) {
            ids.add(aluno.id)
        }
    }
    return ids
}

private fun contarChamadasVisiveis(chamadas: List<Chamada>, turmasVisiveis: Set<String>): Int {
    var quantidade = 0
    for (chamada in chamadas) {
        if (chamada.deletedAt == null && chamada.turmaId in turmasVisiveis) {
            quantidade = quantidade + 1
        }
    }
    return quantidade
}

private fun contarPendenciasEmAberto(pendencias: List<Pendencia>, alunosVisiveis: Set<String>): Int {
    var quantidade = 0
    for (pendencia in pendencias) {
        val visivel: Boolean = pendencia.deletedAt == null && pendencia.alunoId in alunosVisiveis
        if (visivel && pendencia.status == StatusPendencia.PENDENTE) {
            quantidade = quantidade + 1
        }
    }
    return quantidade
}

private fun contarAnotacoesVisiveis(anotacoes: List<Anotacao>, alunosVisiveis: Set<String>): Int {
    var quantidade = 0
    for (anotacao in anotacoes) {
        if (anotacao.deletedAt == null && anotacao.alunoId in alunosVisiveis) {
            quantidade = quantidade + 1
        }
    }
    return quantidade
}
