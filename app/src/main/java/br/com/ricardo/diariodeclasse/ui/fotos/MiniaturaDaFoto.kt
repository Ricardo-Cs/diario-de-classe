package br.com.ricardo.diariodeclasse.ui.fotos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import br.com.ricardo.diariodeclasse.data.local.entity.Foto
import coil3.compose.AsyncImage
import java.io.File

/** A foto e o arquivo da imagem dela, que é o que a tela precisa para exibir. */
data class FotoNaTela(
    val foto: Foto,
    val arquivo: File,
)

/**
 * Miniatura quadrada, recortada no centro. `AsyncImage` (do Coil) lê o arquivo
 * fora da thread da tela e já reduz a imagem ao tamanho exibido, para uma grade
 * com dezenas de fotos não pesar na memória.
 *
 * Se a imagem não existir neste celular (ver `FotoScreen`), aparece um quadrado
 * na cor de fundo.
 */
@Composable
fun MiniaturaDaFoto(
    foto: FotoNaTela,
    descricao: String,
    aoTocar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val corDeFundo = ColorPainter(MaterialTheme.colorScheme.surfaceVariant)

    AsyncImage(
        model = foto.arquivo,
        contentDescription = descricao,
        contentScale = ContentScale.Crop,
        placeholder = corDeFundo,
        error = corDeFundo,
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = aoTocar),
    )
}
