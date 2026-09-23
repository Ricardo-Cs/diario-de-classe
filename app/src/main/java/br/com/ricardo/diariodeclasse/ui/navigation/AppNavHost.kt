package br.com.ricardo.diariodeclasse.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import br.com.ricardo.diariodeclasse.ui.turmas.TurmasScreen

@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = TurmasRoute,
        modifier = modifier,
    ) {
        composable<TurmasRoute> {
            TurmasScreen()
        }
    }
}
