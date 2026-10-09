package br.com.ricardo.diariodeclasse.data.local.entity

/**
 * Resultado de consulta, não é tabela: só o que a notificação diária mostra
 * de cada pendência (quem e o quê) e a turma, para o toque abrir a tela certa.
 */
data class PendenciaParaLembrete(
    val turmaId: String,
    val nomeDoAluno: String,
    val descricao: String,
)
