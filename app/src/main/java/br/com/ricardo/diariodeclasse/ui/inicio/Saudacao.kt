package br.com.ricardo.diariodeclasse.ui.inicio

import java.time.LocalTime

enum class Saudacao { BOM_DIA, BOA_TARDE, BOA_NOITE }

fun saudacaoParaHorario(horario: LocalTime): Saudacao {
    if (horario.hour < 12) {
        return Saudacao.BOM_DIA
    }
    if (horario.hour < 18) {
        return Saudacao.BOA_TARDE
    }
    return Saudacao.BOA_NOITE
}

/**
 * Na saudação vai só o primeiro nome ("Bom dia, Maria!"), como se fala na escola.
 * `null` enquanto ela não preencheu o perfil: a saudação fica sem nome.
 */
fun primeiroNome(nomeCompleto: String?): String? {
    if (nomeCompleto == null || nomeCompleto.isBlank()) {
        return null
    }
    val partes: List<String> = nomeCompleto.trim().split(" ")
    return partes.first()
}
