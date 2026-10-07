package br.com.ricardo.diariodeclasse.ui.metricas

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import br.com.ricardo.diariodeclasse.R
import br.com.ricardo.diariodeclasse.data.local.entity.NivelDaMetrica

/**
 * Painel com os níveis da escala para um aluno. Um toque escolhe e fecha:
 * mudar o nível de um aluno custa dois toques (abrir e escolher).
 *
 * `null` em [nivelEscolhidoId] e em [aoEscolher] é "Não avaliado".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolhaEscolhaDeNivel(
    nomeDoAluno: String,
    niveis: List<NivelDaMetrica>,
    nivelEscolhidoId: String?,
    aoEscolher: (nivelId: String?) -> Unit,
    aoFechar: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = aoFechar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .padding(bottom = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = stringResource(R.string.sondagem_nivel_de, nomeDoAluno),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            )

            for (nivel in niveis) {
                OpcaoDeNivel(
                    texto = nivel.nome,
                    selecionada = nivel.id == nivelEscolhidoId,
                    aoTocar = { aoEscolher(nivel.id) },
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            OpcaoDeNivel(
                texto = stringResource(R.string.sondagem_nao_avaliado),
                selecionada = nivelEscolhidoId == null,
                aoTocar = { aoEscolher(null) },
            )
        }
    }
}

/**
 * `selectable` com `Role.RadioButton` faz a linha inteira ser tocável e o leitor de
 * tela anunciar "selecionado"; o `RadioButton` sem `onClick` é só o desenho.
 */
@Composable
private fun OpcaoDeNivel(texto: String, selecionada: Boolean, aoTocar: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(selected = selecionada, role = Role.RadioButton, onClick = aoTocar)
            .padding(horizontal = 16.dp),
    ) {
        RadioButton(selected = selecionada, onClick = null)
        Spacer(Modifier.width(16.dp))
        Text(text = texto, style = MaterialTheme.typography.bodyLarge)
    }
}
