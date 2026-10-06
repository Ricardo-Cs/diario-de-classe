package br.com.ricardo.diariodeclasse.ui.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy

@Composable
fun BarraInferior(
    destinoAtual: NavDestination?,
    aoSelecionarAba: (aba: AbaPrincipal) -> Unit,
) {
    NavigationBar {
        for (aba in AbaPrincipal.entries) {
            NavigationBarItem(
                selected = estaNaAba(destinoAtual, aba),
                onClick = { aoSelecionarAba(aba) },
                icon = { Icon(painterResource(aba.icone), contentDescription = null) },
                label = { Text(stringResource(aba.titulo)) },
            )
        }
    }
}

/**
 * O destino atual pode ser uma tela dentro do grafo da aba (ex.: detalhe da turma),
 * então subimos pela hierarquia (tela → grafo da aba → grafo raiz) procurando o grafo da aba.
 */
private fun estaNaAba(destinoAtual: NavDestination?, aba: AbaPrincipal): Boolean {
    if (destinoAtual == null) {
        return false
    }
    for (destino in destinoAtual.hierarchy) {
        if (destino.hasRoute(aba.grafo::class)) {
            return true
        }
    }
    return false
}
