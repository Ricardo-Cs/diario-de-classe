package br.com.ricardo.diariodeclasse.ui.componentes

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import br.com.ricardo.diariodeclasse.R

/** Provisória para as abas cujo conteúdo ainda não foi implementado. */
@Composable
fun TelaEmConstrucao(titulo: String) {
    Scaffold(
        topBar = { BarraSuperior(titulo = titulo) },
    ) { espacamentoDasBarras ->
        MensagemCentralizada(
            mensagem = stringResource(R.string.em_construcao),
            modifier = Modifier.padding(espacamentoDasBarras),
        )
    }
}
