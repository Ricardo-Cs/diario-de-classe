package br.com.ricardo.diariodeclasse.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import br.com.ricardo.diariodeclasse.ui.chamada.ChamadaScreen
import br.com.ricardo.diariodeclasse.ui.diario.DiarioScreen
import br.com.ricardo.diariodeclasse.ui.inicio.InicioScreen
import br.com.ricardo.diariodeclasse.ui.mais.MaisScreen
import br.com.ricardo.diariodeclasse.ui.turmas.DetalheTurmaScreen
import br.com.ricardo.diariodeclasse.ui.turmas.FormularioTurmaScreen
import br.com.ricardo.diariodeclasse.ui.turmas.ListaTurmasScreen

/**
 * Mapa de navegação do app. As telas não conhecem o `navController`:
 * recebem funções ("ao abrir turma", "ao voltar") e é aqui que se decide para onde ir.
 */
@Composable
fun AppNavHost() {
    val navController: NavHostController = rememberNavController()
    val entradaAtual: NavBackStackEntry? = navController.currentBackStackEntryAsState().value
    val destinoAtual: NavDestination? = entradaAtual?.destination

    // As telas internas já têm o próprio Scaffold e tratam as bordas do sistema
    // (barra de status etc.). Por isso este Scaffold externo não aplica nenhuma
    // e só reserva o espaço da barra inferior.
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (mostraBarraInferior(destinoAtual)) {
                BarraInferior(
                    destinoAtual = destinoAtual,
                    aoSelecionarAba = { aba -> navegarParaAba(navController, aba) },
                )
            }
        },
    ) { espacamentoDaBarraInferior: PaddingValues ->

        // `consumeWindowInsets` avisa às telas internas que a parte de baixo já foi
        // ocupada pela barra inferior; sem isso elas somariam de novo o espaço
        // da barra de navegação do sistema.
        val modifier = Modifier
            .padding(espacamentoDaBarraInferior)
            .consumeWindowInsets(espacamentoDaBarraInferior)

        NavHost(
            navController = navController,
            startDestination = InicioGrafo,
            modifier = modifier,
        ) {

            navigation<InicioGrafo>(startDestination = InicioRoute) {
                composable<InicioRoute> {
                    InicioScreen(
                        aoCadastrarTurma = { navController.navigate(FormularioTurmaRoute()) },
                        aoAbrirChamada = { turmaId, data ->
                            navController.navigate(ChamadaRoute(turmaId, data.toString()))
                        },
                    )
                }

                composable<ChamadaRoute> {
                    ChamadaScreen(
                        aoVoltar = { navController.popBackStack() },
                    )
                }
            }

            navigation<TurmaGrafo>(startDestination = ListaTurmasRoute) {
                composable<ListaTurmasRoute> {
                    ListaTurmasScreen(
                        aoAbrirTurma = { turmaId -> navController.navigate(DetalheTurmaRoute(turmaId)) },
                        aoCriarTurma = { navController.navigate(FormularioTurmaRoute()) },
                    )
                }

                composable<FormularioTurmaRoute> {
                    FormularioTurmaScreen(
                        aoVoltar = { navController.popBackStack() },
                    )
                }

                composable<DetalheTurmaRoute> {
                    DetalheTurmaScreen(
                        aoEditarTurma = { turmaId -> navController.navigate(FormularioTurmaRoute(turmaId)) },
                        aoVoltar = { navController.popBackStack() },
                    )
                }
            }

            navigation<DiarioGrafo>(startDestination = DiarioRoute) {
                composable<DiarioRoute> {
                    DiarioScreen()
                }
            }

            navigation<MaisGrafo>(startDestination = MaisRoute) {
                composable<MaisRoute> {
                    MaisScreen()
                }
            }
        }
    }
}

/**
 * Em formulários (cadastro de turma, chamada) a barra some, para dar espaço
 * ao teclado e evitar sair no meio do preenchimento.
 */
private fun mostraBarraInferior(destinoAtual: NavDestination?): Boolean {
    // Antes de o NavHost montar a primeira tela, o destino ainda é nulo; o app sempre
    // começa no Início, então já mostramos a barra para ela não "piscar".
    if (destinoAtual == null) {
        return true
    }
    val estaNoFormularioDeTurma: Boolean = destinoAtual.hasRoute(FormularioTurmaRoute::class)
    val estaNaChamada: Boolean = destinoAtual.hasRoute(ChamadaRoute::class)
    if (estaNoFormularioDeTurma || estaNaChamada) {
        return false
    }
    return true
}

/**
 * Troca de aba guardando a pilha da aba atual e restaurando a da aba de destino.
 * Ao voltar a partir da raiz de qualquer aba, o usuário cai no Início antes de sair do app.
 */
private fun navegarParaAba(navController: NavHostController, aba: AbaPrincipal) {
    val telaInicialDoApp: NavDestination = navController.graph.findStartDestination()

    navController.navigate(aba.grafo) {
        popUpTo(telaInicialDoApp.id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
