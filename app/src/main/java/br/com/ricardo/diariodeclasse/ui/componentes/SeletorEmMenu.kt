package br.com.ricardo.diariodeclasse.ui.componentes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import br.com.ricardo.diariodeclasse.R

/** Uma opção do [SeletorEmMenu]: o id que volta para quem escolheu e o texto mostrado. */
data class OpcaoDoMenu(
    val id: String,
    val texto: String,
)

/**
 * Campo com rótulo acima e um botão que abre um menu suspenso com as [opcoes]
 * (mesmo padrão do seletor de aluno das pendências). [textoSemEscolha] aparece
 * enquanto nada foi escolhido.
 */
@Composable
fun SeletorEmMenu(
    rotulo: String,
    opcoes: List<OpcaoDoMenu>,
    idEscolhido: String?,
    textoSemEscolha: String,
    aoEscolher: (id: String) -> Unit,
) {
    val menuAberto: MutableState<Boolean> = remember { mutableStateOf(false) }

    var textoDoBotao: String = textoSemEscolha
    for (opcao in opcoes) {
        if (opcao.id == idEscolhido) {
            textoDoBotao = opcao.texto
        }
    }

    Column {
        Text(
            text = rotulo,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box {
            OutlinedButton(
                onClick = { menuAberto.value = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = textoDoBotao, modifier = Modifier.weight(1f))
                Icon(painterResource(R.drawable.ic_expandir), contentDescription = null)
            }

            DropdownMenu(
                expanded = menuAberto.value,
                onDismissRequest = { menuAberto.value = false },
            ) {
                for (opcao in opcoes) {
                    DropdownMenuItem(
                        text = { Text(opcao.texto) },
                        onClick = {
                            aoEscolher(opcao.id)
                            menuAberto.value = false
                        },
                    )
                }
            }
        }
    }
}
