package br.com.ricardo.diariodeclasse.data.local.entity

import java.time.LocalDate

/** Uma sondagem na lista da métrica: a data e quantos alunos foram avaliados. */
data class SondagemResumida(
    val id: String,
    val data: LocalDate,
    val quantidadeDeAlunos: Int,
)
