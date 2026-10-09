package br.com.ricardo.diariodeclasse.ui.navigation

import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy

/**
 * Barra na cor da marca (Molten Lava). Usa o `primaryContainer`, e não o `primary`,
 * porque no tema escuro o `primary` é um tom rosado; o container é Molten Lava nos dois temas.
 */
@Composable
fun BarraInferior(
    destinoAtual: NavDestination?,
    aoSelecionarAba: (aba: AbaPrincipal) -> Unit,
) {
    val corDaBarra: Color = MaterialTheme.colorScheme.primaryContainer
    val corDoConteudo: Color = MaterialTheme.colorScheme.onPrimaryContainer

    NavigationBar(
        containerColor = corDaBarra,
        contentColor = corDoConteudo,
    ) {
        for (aba in AbaPrincipal.entries) {
            NavigationBarItem(
                selected = estaNaAba(destinoAtual, aba),
                onClick = { aoSelecionarAba(aba) },
                icon = { Icon(painterResource(aba.icone), contentDescription = null) },
                label = { RotuloDaAba(stringResource(aba.titulo)) },
                colors = coresDosItens(corDaBarra, corDoConteudo),
            )
        }
    }
}

/**
 * Sempre numa linha só. Em celular pequeno com fonte grande, "Acompanhar" não
 * caberia: em vez de quebrar a palavra, a letra diminui até caber (no mínimo 9sp).
 */
@Composable
private fun RotuloDaAba(texto: String) {
    Text(
        text = texto,
        maxLines = 1,
        autoSize = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = MaterialTheme.typography.labelMedium.fontSize),
    )
}

/**
 * Aba selecionada: "pílula" clara com o ícone na cor da barra.
 * Abas não selecionadas: ícone e texto claros, um pouco apagados.
 */
@Composable
private fun coresDosItens(corDaBarra: Color, corDoConteudo: Color): NavigationBarItemColors {
    val corApagada: Color = corDoConteudo.copy(alpha = 0.7f)

    return NavigationBarItemDefaults.colors(
        selectedIconColor = corDaBarra,
        indicatorColor = corDoConteudo,
        selectedTextColor = corDoConteudo,
        unselectedIconColor = corApagada,
        unselectedTextColor = corApagada,
    )
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
