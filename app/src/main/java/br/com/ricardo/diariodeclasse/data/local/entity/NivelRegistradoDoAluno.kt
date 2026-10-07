package br.com.ricardo.diariodeclasse.data.local.entity

import java.time.LocalDate

/** Um nível registrado para o aluno numa sondagem, já com os nomes para a tela do aluno. */
data class NivelRegistradoDoAluno(
    val metricaId: String,
    val nomeDaMetrica: String,
    val nivelId: String,
    val nomeDoNivel: String,
    val data: LocalDate,
)
