package br.com.ricardo.diariodeclasse.ui.componentes

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import br.com.ricardo.diariodeclasse.R

@Composable
fun DialogoConfirmacao(
    titulo: String,
    mensagem: String,
    textoConfirmar: String,
    aoConfirmar: () -> Unit,
    aoCancelar: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = aoCancelar,
        title = { Text(titulo) },
        text = { Text(mensagem) },
        confirmButton = {
            TextButton(onClick = aoConfirmar) {
                Text(textoConfirmar)
            }
        },
        dismissButton = {
            TextButton(onClick = aoCancelar) {
                Text(stringResource(R.string.cancelar))
            }
        },
    )
}
