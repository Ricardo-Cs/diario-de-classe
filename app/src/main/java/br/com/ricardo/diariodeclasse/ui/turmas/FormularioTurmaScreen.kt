package br.com.ricardo.diariodeclasse.ui.turmas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.ricardo.diariodeclasse.R
import br.com.ricardo.diariodeclasse.data.local.entity.Periodo
import br.com.ricardo.diariodeclasse.ui.componentes.BarraSuperior
import br.com.ricardo.diariodeclasse.ui.componentes.nomeDoPeriodo

@Composable
fun FormularioTurmaScreen(
    aoVoltar: () -> Unit,
    viewModel: FormularioTurmaViewModel = hiltViewModel(),
) {
    val estado: FormularioTurmaUiState = viewModel.uiState.collectAsStateWithLifecycle().value

    LaunchedEffect(estado.salvo) {
        if (estado.salvo) {
            aoVoltar()
        }
    }

    val titulo: String = if (estado.editando) {
        stringResource(R.string.turma_formulario_titulo_edicao)
    } else {
        stringResource(R.string.turma_formulario_titulo_nova)
    }

    Scaffold(
        topBar = { BarraSuperior(titulo = titulo, aoVoltar = aoVoltar) },
    ) { espacamentoDasBarras ->
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .padding(espacamentoDasBarras)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            OutlinedTextField(
                value = estado.nome,
                onValueChange = { texto -> viewModel.alterarNome(texto) },
                label = { Text(stringResource(R.string.turma_campo_nome)) },
                placeholder = { Text(stringResource(R.string.turma_campo_nome_exemplo)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = estado.anoSerie,
                onValueChange = { texto -> viewModel.alterarAnoSerie(texto) },
                label = { Text(stringResource(R.string.turma_campo_ano_serie)) },
                placeholder = { Text(stringResource(R.string.turma_campo_ano_serie_exemplo)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            SeletorDePeriodo(
                periodoSelecionado = estado.periodo,
                aoSelecionar = { periodo -> viewModel.alterarPeriodo(periodo) },
            )

            OutlinedTextField(
                value = estado.anoLetivo,
                onValueChange = { texto -> viewModel.alterarAnoLetivo(texto) },
                label = { Text(stringResource(R.string.turma_campo_ano_letivo)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = { viewModel.salvar() },
                enabled = estado.podeSalvar(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.salvar))
            }
        }
    }
}

@Composable
private fun SeletorDePeriodo(
    periodoSelecionado: Periodo,
    aoSelecionar: (Periodo) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.turma_campo_periodo),
            style = MaterialTheme.typography.labelLarge,
        )

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (periodo in Periodo.entries) {
                FilterChip(
                    selected = periodo == periodoSelecionado,
                    onClick = { aoSelecionar(periodo) },
                    label = { Text(nomeDoPeriodo(periodo)) },
                )
            }
        }
    }
}
