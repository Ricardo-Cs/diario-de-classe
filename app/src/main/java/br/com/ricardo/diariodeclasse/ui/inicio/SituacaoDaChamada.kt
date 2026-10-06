package br.com.ricardo.diariodeclasse.ui.inicio

import br.com.ricardo.diariodeclasse.data.local.entity.Aluno
import br.com.ricardo.diariodeclasse.data.local.entity.RegistroPresenca
import br.com.ricardo.diariodeclasse.data.repository.ChamadaDoDia
import java.time.LocalTime
import java.time.ZoneId

/** O que o card da chamada no Início precisa mostrar. */
sealed interface SituacaoDaChamada {
    data object TurmaSemAlunos : SituacaoDaChamada
    data class NaoFeita(val totalDeAlunos: Int) : SituacaoDaChamada
    data class Feita(val horario: LocalTime, val presentes: Int, val ausentes: Int) : SituacaoDaChamada
}

/**
 * As contagens partem dos alunos atuais da turma, como a tela de chamada:
 * aluno cadastrado depois da chamada conta como presente, e aluno excluído não conta.
 */
fun calcularSituacaoDaChamada(
    alunos: List<Aluno>,
    chamadaDeHoje: ChamadaDoDia?,
    fusoHorario: ZoneId,
): SituacaoDaChamada {
    if (alunos.isEmpty()) {
        return SituacaoDaChamada.TurmaSemAlunos
    }
    if (chamadaDeHoje == null) {
        return SituacaoDaChamada.NaoFeita(totalDeAlunos = alunos.size)
    }

    var ausentes = 0
    for (aluno in alunos) {
        if (faltou(aluno, chamadaDeHoje.registros)) {
            ausentes = ausentes + 1
        }
    }

    val horario: LocalTime = chamadaDeHoje.chamada.createdAt.atZone(fusoHorario).toLocalTime()
    return SituacaoDaChamada.Feita(
        horario = horario,
        presentes = alunos.size - ausentes,
        ausentes = ausentes,
    )
}

private fun faltou(aluno: Aluno, registros: List<RegistroPresenca>): Boolean {
    for (registro in registros) {
        if (registro.alunoId == aluno.id) {
            return !registro.presente
        }
    }
    return false
}
