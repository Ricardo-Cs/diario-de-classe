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

/**
 * A data vai como texto ("2026-10-06") porque a rota precisa ser serializável
 * e o `LocalDate` não é por padrão.
 */
@Serializable
data class ChamadaRoute(val turmaId: String, val data: String)

@Serializable
data class PendenciasRoute(val turmaId: String)

@Serializable
data class AlunoRoute(val alunoId: String)

/**
 * A mesma tela do aluno, aberta a partir da aba Início (pela tela de pendências).
 * Cada aba tem a própria pilha de telas: com uma rota própria do Início, o aluno
 * abre nessa pilha, "voltar" retorna às pendências e a barra inferior continua
 * destacando o Início.
 */
@Serializable
data class AlunoNoInicioRoute(val alunoId: String)

/** Lembretes da professora; fica na pilha do Início, onde fica o card deles. */
@Serializable
object LembretesRoute

/** A mesma tela do aluno, aberta a partir da aba Diário (pela tela da meta). */
@Serializable
data class AlunoNoDiarioRoute(val alunoId: String)

/** `metricaId == null` significa "criar nova métrica". */
@Serializable
data class FormularioMetricaRoute(val turmaId: String, val metricaId: String? = null)

@Serializable
data class MetricaRoute(val metricaId: String)

/** A data vai como texto, pelo mesmo motivo da [ChamadaRoute]. */
@Serializable
data class SondagemRoute(val metricaId: String, val data: String)

/** `metaId == null` significa "criar nova meta". */
@Serializable
data class FormularioMetaRoute(val turmaId: String, val metaId: String? = null)

@Serializable
data class MetaRoute(val metaId: String)

/** Linha do tempo das fotos da turma; fica na pilha do Diário, onde fica o card de fotos. */
@Serializable
data class FotosRoute(val turmaId: String)

/** Foto em tela cheia; [turmaId] permite deslizar para as outras fotos da turma. */
@Serializable
data class FotoRoute(val turmaId: String, val fotoId: String)
