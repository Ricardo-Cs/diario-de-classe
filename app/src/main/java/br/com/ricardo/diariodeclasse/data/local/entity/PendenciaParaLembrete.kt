package br.com.ricardo.diariodeclasse.data.local.entity

/**
 * Resultado de consulta, não é tabela: só o que a notificação diária mostra
 * de cada pendência (quem e o quê).
 */
data class PendenciaParaLembrete(
    val nomeDoAluno: String,
    val descricao: String,
)
