package br.com.ricardo.diariodeclasse.data.local.entity

/**
 * Resultado de consulta, não é tabela: os números do perfil
 * ("2 turmas · 47 alunos · 120 anotações"), sempre calculados.
 */
data class ResumoDoPerfil(
    val turmas: Int,
    val alunos: Int,
    val anotacoes: Int,
)
