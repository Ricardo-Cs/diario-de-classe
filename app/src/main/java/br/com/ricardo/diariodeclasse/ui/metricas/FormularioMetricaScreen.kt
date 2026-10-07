package br.com.ricardo.diariodeclasse.ui.metricas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.ricardo.diariodeclasse.R
import br.com.ricardo.diariodeclasse.ui.componentes.BarraSuperior
import br.com.ricardo.diariodeclasse.ui.componentes.DialogoConfirmacao

@Composable
fun FormularioMetricaScreen(
    aoVoltar: () -> Unit,
    aoExcluirMetrica: () -> Unit,
    viewModel: FormularioMetricaViewModel = hiltViewModel(),
) {
    val estado: FormularioMetricaUiState = viewModel.uiState.collectAsStateWithLifecycle().value
    val confirmandoExclusao: MutableState<Boolean> = remember { mutableStateOf(false) }

    LaunchedEffect(estado.salvo) {
        if (estado.salvo) {
            aoVoltar()
        }
    }
    LaunchedEffect(estado.excluida) {
        if (estado.excluida) {
            aoExcluirMetrica()
        }
    }

    val titulo: String
    if (estado.editando) {
        titulo = stringResource(R.string.metrica_formulario_titulo_edicao)
    } else {
        titulo = stringResource(R.string.metrica_formulario_titulo_nova)
    }

    // `stringArrayResource` lê a lista do strings.xml; só pode ser chamada dentro de um @Composable.
    val nomeDoModelo: String = stringResource(R.string.metrica_modelo_nome)
    val niveisDoModelo: List<String> = stringArrayResource(R.array.metrica_modelo_niveis).toList()

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
            if (!estado.editando) {
                CartaoDoModelo(aoUsar = { viewModel.usarModelo(nomeDoModelo, niveisDoModelo) })
            }

            OutlinedTextField(
                value = estado.nome,
                onValueChange = { texto -> viewModel.alterarNome(texto) },
                label = { Text(stringResource(R.string.metrica_campo_nome)) },
                placeholder = { Text(stringResource(R.string.metrica_campo_nome_exemplo)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            ListaDeNiveis(estado = estado, viewModel = viewModel)

            Button(
                onClick = { viewModel.salvar() },
                enabled = estado.podeSalvar(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.salvar))
            }

            if (estado.editando) {
                TextButton(
                    onClick = { confirmandoExclusao.value = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.metrica_excluir))
                }
            }
        }
    }

    if (confirmandoExclusao.value) {
        DialogoConfirmacao(
            titulo = stringResource(R.string.metrica_excluir_titulo),
            mensagem = stringResource(R.string.metrica_excluir_mensagem, estado.nome),
            textoConfirmar = stringResource(R.string.excluir),
            aoConfirmar = {
                confirmandoExclusao.value = false
                viewModel.excluir()
            },
            aoCancelar = { confirmandoExclusao.value = false },
        )
    }
}

/** Atalho para a métrica mais comum: os 5 níveis de escrita, já na ordem. */
@Composable
private fun CartaoDoModelo(aoUsar: () -> Unit) {
    OutlinedCard(
        onClick = aoUsar,
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.metrica_usar_modelo),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(
                text = stringResource(R.string.metrica_usar_modelo_descricao),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ListaDeNiveis(estado: FormularioMetricaUiState, viewModel: FormularioMetricaViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.metrica_niveis),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        val ultimaPosicao: Int = estado.niveis.size - 1
        for (posicao in estado.niveis.indices) {
            val nivel: NivelNoFormulario = estado.niveis[posicao]
            // `key` faz o Compose acompanhar cada linha pela chave, e não pela posição:
            // ao reordenar, o campo de texto "anda" junto com o nível.
            key(nivel.chave) {
                LinhaDoNivel(
                    nivel = nivel,
                    numero = posicao + 1,
                    podeSubir = posicao > 0,
                    podeDescer = posicao < ultimaPosicao,
                    viewModel = viewModel,
                )
            }
        }

        TextButton(onClick = { viewModel.adicionarNivel() }) {
            Icon(painter = painterResource(R.drawable.ic_adicionar), contentDescription = null)
            Text(stringResource(R.string.metrica_adicionar_nivel))
        }

        if (!estado.escalaEhValida()) {
            TextoDeAjuda(stringResource(R.string.metrica_precisa_de_dois_niveis))
        }
        if (estado.temNivelEmUso()) {
            TextoDeAjuda(stringResource(R.string.metrica_niveis_em_uso))
        }
    }
}

@Composable
private fun LinhaDoNivel(
    nivel: NivelNoFormulario,
    numero: Int,
    podeSubir: Boolean,
    podeDescer: Boolean,
    viewModel: FormularioMetricaViewModel,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = nivel.nome,
            onValueChange = { texto -> viewModel.alterarNomeDoNivel(nivel.chave, texto) },
            label = { Text(stringResource(R.string.metrica_campo_nivel, numero)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { viewModel.subirNivel(nivel.chave) }, enabled = podeSubir) {
            Icon(
                painter = painterResource(R.drawable.ic_subir),
                contentDescription = stringResource(R.string.metrica_subir_nivel, nivel.nome),
            )
        }
        IconButton(onClick = { viewModel.descerNivel(nivel.chave) }, enabled = podeDescer) {
            Icon(
                painter = painterResource(R.drawable.ic_descer),
                contentDescription = stringResource(R.string.metrica_descer_nivel, nivel.nome),
            )
        }
        IconButton(onClick = { viewModel.removerNivel(nivel.chave) }, enabled = !nivel.emUso) {
            Icon(
                painter = painterResource(R.drawable.ic_excluir),
                contentDescription = stringResource(R.string.metrica_remover_nivel, nivel.nome),
            )
        }
    }
}

@Composable
private fun TextoDeAjuda(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
