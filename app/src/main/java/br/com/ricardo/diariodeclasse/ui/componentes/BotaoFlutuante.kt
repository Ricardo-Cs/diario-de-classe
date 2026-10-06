package br.com.ricardo.diariodeclasse.ui.componentes

import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/**
 * Botão flutuante das telas (ex.: "Nova turma"). Por padrão o Material o pinta com
 * o `primaryContainer`, que é o vermelho da barra inferior; logo acima dela, os dois
 * se confundiam. Em azul-marinho (`secondary`) ele se destaca da barra.
 */
@Composable
fun BotaoFlutuante(texto: String, aoClicar: () -> Unit) {
    ExtendedFloatingActionButton(
        onClick = aoClicar,
        containerColor = MaterialTheme.colorScheme.secondary,
        contentColor = MaterialTheme.colorScheme.onSecondary,
    ) {
        Text(texto)
    }
}
