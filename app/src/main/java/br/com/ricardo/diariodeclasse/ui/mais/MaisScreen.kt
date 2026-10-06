package br.com.ricardo.diariodeclasse.ui.mais

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import br.com.ricardo.diariodeclasse.R
import br.com.ricardo.diariodeclasse.ui.componentes.TelaEmConstrucao

@Composable
fun MaisScreen() {
    TelaEmConstrucao(titulo = stringResource(R.string.aba_mais))
}
