package br.com.ricardo.diariodeclasse.ui.inicio

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.com.ricardo.diariodeclasse.R
import br.com.ricardo.diariodeclasse.data.local.entity.Aluno

/**
 * Primeiro passo do atalho "Anotar": a lista de alunos da turma ativa. Um toque
 * no nome já leva ao campo de texto, sem menu para abrir antes.
 *
 * `ModalBottomSheet` ainda é experimental no Material 3; o `@OptIn` fica só aqui.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolhaEscolhaDeAluno(
    alunos: List<Aluno>,
    aoEscolher: (aluno: Aluno) -> Unit,
    aoFechar: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = aoFechar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Text(
            text = stringResource(R.string.inicio_anotar_sobre_quem),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        )
        LazyColumn(modifier = Modifier.padding(bottom = 16.dp)) {
            items(alunos, key = { aluno -> aluno.id }) { aluno ->
                ListItem(
                    headlineContent = { Text(aluno.nome) },
                    // Transparente para ficar da cor da folha, e não de um cartão por cima dela.
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable(onClick = { aoEscolher(aluno) }),
                )
            }
        }
    }
}
