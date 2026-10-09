package br.com.ricardo.diariodeclasse.ui.perfil

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.com.ricardo.diariodeclasse.R

/**
 * Opções ao tocar na foto do perfil: câmera, galeria e, se já houver foto, remover.
 *
 * `ModalBottomSheet` ainda é experimental no Material 3; o `@OptIn` fica só aqui.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolhaFotoDoPerfil(
    temFoto: Boolean,
    aoTirarFoto: () -> Unit,
    aoEscolherDaGaleria: () -> Unit,
    aoRemover: () -> Unit,
    aoFechar: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = aoFechar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Text(
            text = stringResource(R.string.perfil_foto),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        )
        OpcaoDaFoto(
            icone = R.drawable.ic_camera,
            texto = stringResource(R.string.fotos_tirar_foto),
            cor = MaterialTheme.colorScheme.onSurface,
            aoTocar = aoTirarFoto,
        )
        OpcaoDaFoto(
            icone = R.drawable.ic_galeria,
            texto = stringResource(R.string.perfil_escolher_da_galeria),
            cor = MaterialTheme.colorScheme.onSurface,
            aoTocar = aoEscolherDaGaleria,
        )
        if (temFoto) {
            OpcaoDaFoto(
                icone = R.drawable.ic_excluir,
                texto = stringResource(R.string.perfil_remover_foto),
                cor = MaterialTheme.colorScheme.error,
                aoTocar = aoRemover,
            )
        }
    }
}

@Composable
private fun OpcaoDaFoto(icone: Int, texto: String, cor: Color, aoTocar: () -> Unit) {
    ListItem(
        leadingContent = { Icon(painterResource(icone), contentDescription = null, tint = cor) },
        headlineContent = { Text(text = texto, color = cor) },
        // Transparente para ficar da cor da folha, e não de um cartão por cima dela.
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = aoTocar),
    )
}
