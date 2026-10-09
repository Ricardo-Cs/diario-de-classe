package br.com.ricardo.diariodeclasse.ui.acompanhamento

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.com.ricardo.diariodeclasse.R
import br.com.ricardo.diariodeclasse.ui.fotos.BotoesDeOrigem
import br.com.ricardo.diariodeclasse.ui.fotos.MiniaturaDaFoto
import br.com.ricardo.diariodeclasse.ui.fotos.descricaoDaFoto

/**
 * Card "Fotos do dia": as fotos de hoje e os botões para adicionar mais. Daqui
 * as fotos entram sempre no dia de hoje; para outro dia, a tela de fotos da
 * turma deixa escolher a data.
 */
@Composable
fun SecaoDeFotos(
    estado: AcompanhamentoUiState.Carregado,
    salvando: Boolean,
    aoTirarFoto: () -> Unit,
    aoEscolherDaGaleria: () -> Unit,
    aoAbrirFoto: (fotoId: String) -> Unit,
    aoVerTodas: () -> Unit,
) {
    Text(
        text = stringResource(R.string.diario_fotos),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(top = 8.dp),
    )

    OutlinedCard(
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (salvando) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            if (estado.fotosDeHoje.isEmpty()) {
                Text(
                    text = stringResource(R.string.diario_sem_fotos_hoje),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                FotosDeHoje(estado = estado, aoAbrirFoto = aoAbrirFoto)
            }

            BotoesDeOrigem(
                aoTirarFoto = aoTirarFoto,
                aoEscolherDaGaleria = aoEscolherDaGaleria,
                habilitados = !salvando,
            )

            if (estado.temFotos) {
                TextButton(onClick = aoVerTodas) {
                    Text(stringResource(R.string.diario_ver_fotos))
                }
            }
        }
    }
}

/** "3 fotos hoje" e as miniaturas numa fileira que rola para o lado. */
@Composable
private fun FotosDeHoje(estado: AcompanhamentoUiState.Carregado, aoAbrirFoto: (fotoId: String) -> Unit) {
    val quantidade: Int = estado.fotosDeHoje.size

    Text(
        text = pluralStringResource(R.plurals.diario_fotos_hoje, quantidade, quantidade),
        style = MaterialTheme.typography.bodyMedium,
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(estado.fotosDeHoje, key = { foto -> foto.foto.id }) { foto ->
            MiniaturaDaFoto(
                foto = foto,
                descricao = descricaoDaFoto(foto, estado.hoje),
                aoTocar = { aoAbrirFoto(foto.foto.id) },
                modifier = Modifier.size(72.dp),
            )
        }
    }
}
