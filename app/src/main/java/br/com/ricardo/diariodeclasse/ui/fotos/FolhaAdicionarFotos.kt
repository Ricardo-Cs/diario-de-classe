package br.com.ricardo.diariodeclasse.ui.fotos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.com.ricardo.diariodeclasse.R
import br.com.ricardo.diariodeclasse.ui.componentes.DialogoCalendario
import br.com.ricardo.diariodeclasse.ui.componentes.formatarDataPorExtenso
import java.time.LocalDate

/**
 * Painel de "Adicionar fotos": o dia (hoje por padrão; dá para escolher um dia
 * anterior, para fotos que ficaram na galeria) e de onde vêm as fotos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolhaAdicionarFotos(
    hoje: LocalDate,
    aoTirarFoto: (data: LocalDate) -> Unit,
    aoEscolherDaGaleria: (data: LocalDate) -> Unit,
    aoFechar: () -> Unit,
) {
    val data: MutableState<LocalDate> = remember { mutableStateOf(hoje) }
    val escolhendoData: MutableState<Boolean> = remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = aoFechar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        ) {
            Text(text = stringResource(R.string.fotos_adicionar), style = MaterialTheme.typography.titleMedium)

            Column {
                Text(
                    text = stringResource(R.string.fotos_campo_dia),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(onClick = { escolhendoData.value = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(text = formatarDataPorExtenso(data.value), modifier = Modifier.weight(1f))
                }
            }

            BotoesDeOrigem(
                aoTirarFoto = { aoTirarFoto(data.value) },
                aoEscolherDaGaleria = { aoEscolherDaGaleria(data.value) },
            )
        }
    }

    if (escolhendoData.value) {
        DialogoCalendario(
            dataInicial = data.value,
            ultimaDataPermitida = hoje,
            aoEscolher = { escolhida ->
                data.value = escolhida
                escolhendoData.value = false
            },
            aoCancelar = { escolhendoData.value = false },
        )
    }
}

/** "Tirar foto" e "Da galeria", lado a lado. Também usados no card do Acompanhamento. */
@Composable
fun BotoesDeOrigem(
    aoTirarFoto: () -> Unit,
    aoEscolherDaGaleria: () -> Unit,
    habilitados: Boolean = true,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        BotaoComIcone(
            icone = R.drawable.ic_camera,
            texto = stringResource(R.string.fotos_tirar_foto),
            habilitado = habilitados,
            aoTocar = aoTirarFoto,
            modifier = Modifier.weight(1f),
        )
        BotaoComIcone(
            icone = R.drawable.ic_galeria,
            texto = stringResource(R.string.fotos_da_galeria),
            habilitado = habilitados,
            aoTocar = aoEscolherDaGaleria,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun BotaoComIcone(
    icone: Int,
    texto: String,
    habilitado: Boolean,
    aoTocar: () -> Unit,
    modifier: Modifier,
) {
    OutlinedButton(onClick = aoTocar, enabled = habilitado, modifier = modifier) {
        Icon(
            painter = painterResource(icone),
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.IconSize),
        )
        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        Text(texto)
    }
}
