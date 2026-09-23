package br.com.ricardo.diariodeclasse.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import br.com.ricardo.diariodeclasse.ui.turmas.DetalheTurmaScreen
import br.com.ricardo.diariodeclasse.ui.turmas.FormularioTurmaScreen
import br.com.ricardo.diariodeclasse.ui.turmas.ListaTurmasScreen

/**
 * Mapa de navegação do app. As telas não conhecem o `navController`:
 * recebem funções ("ao abrir turma", "ao voltar") e é aqui que se decide para onde ir.
 */
@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = ListaTurmasRoute) {

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
}
