package br.com.ricardo.diariodeclasse.ui.navigation

import kotlinx.serialization.Serializable

/**
 * Rotas tipadas do Navigation Compose: cada destino é uma classe/objeto serializável.
 * Rotas com parâmetros viram `data class` (ex.: `data class DetalheTurmaRoute(val turmaId: String)`).
 */
@Serializable
object TurmasRoute
