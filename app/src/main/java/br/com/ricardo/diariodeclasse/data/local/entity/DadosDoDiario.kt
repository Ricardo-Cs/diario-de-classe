package br.com.ricardo.diariodeclasse.data.local.entity

/** Todas as linhas de todas as tabelas: o conteúdo completo do diário. */
data class DadosDoDiario(
    val turmas: List<Turma>,
    val alunos: List<Aluno>,
    val chamadas: List<Chamada>,
    val registrosPresenca: List<RegistroPresenca>,
    val pendencias: List<Pendencia>,
    val anotacoes: List<Anotacao>,
)
