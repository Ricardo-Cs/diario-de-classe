package br.com.ricardo.diariodeclasse.ui.perfil

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.ricardo.diariodeclasse.R
import coil3.compose.AsyncImage
import java.io.File

/**
 * Foto redonda da professora. Sem foto (ou com a imagem ausente neste celular,
 * ex.: depois do backup automático, que não leva as fotos), mostra as iniciais
 * do nome; sem nome, um ícone de pessoa.
 */
@Composable
fun FotoDoPerfil(
    arquivo: File?,
    nome: String,
    tamanho: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(tamanho)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
    ) {
        if (arquivo != null && arquivo.exists()) {
            AsyncImage(
                model = arquivo,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            SemFoto(nome = nome, tamanho = tamanho)
        }
    }
}

@Composable
private fun SemFoto(nome: String, tamanho: Dp) {
    val iniciais: String = iniciaisDoNome(nome)
    if (iniciais.isEmpty()) {
        Icon(
            painter = painterResource(R.drawable.ic_perfil),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(tamanho * 0.6f),
        )
        return
    }
    // As letras ocupam cerca de 40% do círculo, em qualquer tamanho de foto.
    val tamanhoDaLetra: TextUnit = (tamanho.value * 0.4f).sp
    Text(
        text = iniciais,
        style = TextStyle(fontSize = tamanhoDaLetra),
        color = MaterialTheme.colorScheme.onSecondaryContainer,
    )
}

/** "Maria Souza" → "MS"; "Maria" → "M"; "" → "". */
fun iniciaisDoNome(nome: String): String {
    val partes: List<String> = nome.trim().split(" ").filter { parte -> parte.isNotEmpty() }
    if (partes.isEmpty()) {
        return ""
    }
    val primeira: Char = partes.first().first()
    if (partes.size == 1) {
        return primeira.uppercase()
    }
    val ultima: Char = partes.last().first()
    return primeira.uppercase() + ultima.uppercase()
}

/**
 * Ícone da aba Perfil, como no Instagram: a foto (ou as iniciais) dela no lugar
 * do desenho. Antes de ela preencher o perfil, fica o ícone de pessoa, com a
 * mesma cor dos ícones das outras abas.
 *
 * Fica fora do NavHost, então este ViewModel é o da Activity, separado do da
 * tela de perfil; os dois leem o mesmo perfil do banco.
 */
@Composable
fun FotoDoPerfilNaBarra(viewModel: PerfilViewModel = hiltViewModel()) {
    val estado: PerfilUiState = viewModel.uiState.collectAsStateWithLifecycle().value

    if (estado.arquivoDaFoto == null && estado.nome.isBlank()) {
        Icon(painter = painterResource(R.drawable.ic_perfil), contentDescription = null)
        return
    }
    // Mesmo tamanho dos ícones das outras abas, para os rótulos ficarem alinhados.
    FotoDoPerfil(arquivo = estado.arquivoDaFoto, nome = estado.nome, tamanho = 24.dp)
}
