package br.com.ricardo.diariodeclasse.ui.navigation

import kotlinx.serialization.Serializable

/**
 * Rotas tipadas do Navigation Compose: cada tela é uma classe serializável.
 * Os parâmetros da rota viram propriedades da classe.
 */

// Grafos das abas: cada aba é um grafo aninhado com a própria pilha de telas.

@Serializable
object InicioGrafo

@Serializable
object TurmaGrafo

@Serializable
object DiarioGrafo

@Serializable
object MaisGrafo

// Telas

@Serializable
object InicioRoute

@Serializable
object ListaTurmasRoute

/** `turmaId == null` significa "criar nova turma". */
@Serializable
data class FormularioTurmaRoute(val turmaId: String? = null)

@Serializable
data class DetalheTurmaRoute(val turmaId: String)

@Serializable
object DiarioRoute

@Serializable
object MaisRoute
