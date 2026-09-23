package br.com.ricardo.diariodeclasse.ui.componentes

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import br.com.ricardo.diariodeclasse.R

/**
 * Barra do topo das telas. Sem [aoVoltar], a seta de voltar não aparece.
 * O `TopAppBar` do Material 3 ainda é marcado como experimental; o `@OptIn` fica só aqui.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarraSuperior(
    titulo: String,
    aoVoltar: (() -> Unit)? = null,
    acoes: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = { Text(titulo) },
        navigationIcon = {
            if (aoVoltar != null) {
                IconButton(onClick = aoVoltar) {
                    Icon(
                        painter = painterResource(R.drawable.ic_voltar),
                        contentDescription = stringResource(R.string.voltar),
                    )
                }
            }
        },
        actions = acoes,
    )
}
