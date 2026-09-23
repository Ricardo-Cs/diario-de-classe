package br.com.ricardo.diariodeclasse.ui.navigation

import kotlinx.serialization.Serializable

/**
 * Rotas tipadas do Navigation Compose: cada tela é uma classe serializável.
 * Os parâmetros da rota viram propriedades da classe.
 */
@Serializable
object ListaTurmasRoute

/** `turmaId == null` significa "criar nova turma". */
@Serializable
data class FormularioTurmaRoute(val turmaId: String? = null)

@Serializable
data class DetalheTurmaRoute(val turmaId: String)
