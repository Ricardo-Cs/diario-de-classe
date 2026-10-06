package br.com.ricardo.diariodeclasse.ui.inicio

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import br.com.ricardo.diariodeclasse.R
import br.com.ricardo.diariodeclasse.ui.componentes.TelaEmConstrucao

@Composable
fun InicioScreen() {
    TelaEmConstrucao(titulo = stringResource(R.string.aba_inicio))
}
