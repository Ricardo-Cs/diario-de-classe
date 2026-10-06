package br.com.ricardo.diariodeclasse.ui.chamada

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import br.com.ricardo.diariodeclasse.R
import br.com.ricardo.diariodeclasse.ui.pendencias.EscolhaDoLembrete
import java.time.LocalDate

/**
 * Aparece logo depois de salvar uma chamada com ausentes. A chamada já está
 * gravada: tudo aqui é opcional, e fechar o painel não desfaz nada.
 *
 * A mesma atividade pode ir para vários ausentes de uma vez (todos marcados por
 * padrão, exceto quem já tem pendência desta falta). O lembrete começa no dia
 * seguinte à falta, quando o aluno normalmente volta (ver [lembreteInicialDaFalta]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolhaPendenciasDaFalta(
    dataDaChamada: LocalDate,
    hoje: LocalDate,
    etapa: EtapaDaChamada.OferecendoPendencias,
    aoAdicionar: (descricao: String, registroPresencaIds: List<String>, dataLembrete: LocalDate) -> Unit,
    aoConcluir: () -> Unit,
) {
    val descricao: MutableState<String> = remember { mutableStateOf("") }
    val selecionados: MutableState<List<String>> = remember { mutableStateOf(selecaoInicial(etapa.ausentes)) }
    val dataLembrete: MutableState<LocalDate> = remember { mutableStateOf(lembreteInicialDaFalta(dataDaChamada, hoje)) }

    val podeAdicionar: Boolean = descricao.value.isNotBlank() && selecionados.value.isNotEmpty()

    ModalBottomSheet(
        onDismissRequest = aoConcluir,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                .imePadding()
                .verticalScroll(rememberScrollState()),
        ) {
            Column {
                Text(
                    text = stringResource(R.string.chamada_pendencias_titulo),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.chamada_pendencias_explicacao),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            OutlinedTextField(
                value = descricao.value,
                onValueChange = { texto -> descricao.value = texto },
                label = { Text(stringResource(R.string.pendencia_campo_descricao)) },
                placeholder = { Text(stringResource(R.string.pendencia_campo_descricao_exemplo)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            EscolhaDosAusentes(
                ausentes = etapa.ausentes,
                selecionados = selecionados.value,
                aoAlternar = { registroPresencaId ->
                    selecionados.value = alternar(selecionados.value, registroPresencaId)
                },
            )

            EscolhaDoLembrete(
                hoje = hoje,
                dataEscolhida = dataLembrete.value,
                aoEscolher = { data -> dataLembrete.value = data },
            )

            AtividadesJaAdicionadas(etapa.adicionadas)

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = aoConcluir) {
                    if (etapa.adicionadas.isEmpty()) {
                        Text(stringResource(R.string.chamada_pendencias_pular))
                    } else {
                        Text(stringResource(R.string.concluir))
                    }
                }
                Spacer(Modifier.weight(1f))
                Button(
                    enabled = podeAdicionar,
                    onClick = {
                        aoAdicionar(descricao.value, selecionados.value, dataLembrete.value)
                        descricao.value = ""
                    },
                ) {
                    Text(stringResource(R.string.chamada_pendencias_adicionar))
                }
            }
        }
    }
}

/** Quem já tem pendência desta falta começa desmarcado, para não duplicar. */
private fun selecaoInicial(ausentes: List<AusenteDaChamada>): List<String> {
    val ids = mutableListOf<String>()
    for (ausente in ausentes) {
        if (!ausente.jaTemPendencia) {
            ids.add(ausente.registroPresencaId)
        }
    }
    return ids
}

private fun alternar(selecionados: List<String>, id: String): List<String> {
    if (id in selecionados) {
        return selecionados - id
    }
    return selecionados + id
}

/** `FlowRow` quebra os chips em várias linhas quando não cabem na largura da tela. */
@Composable
private fun EscolhaDosAusentes(
    ausentes: List<AusenteDaChamada>,
    selecionados: List<String>,
    aoAlternar: (registroPresencaId: String) -> Unit,
) {
    Column {
        Text(
            text = stringResource(R.string.chamada_pendencias_para_quem),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (ausente in ausentes) {
                val rotulo: String
                if (ausente.jaTemPendencia) {
                    rotulo = stringResource(R.string.chamada_pendencias_ja_tem, ausente.aluno.nome)
                } else {
                    rotulo = ausente.aluno.nome
                }
                FilterChip(
                    selected = ausente.registroPresencaId in selecionados,
                    onClick = { aoAlternar(ausente.registroPresencaId) },
                    label = { Text(rotulo) },
                )
            }
        }
    }
}

@Composable
private fun AtividadesJaAdicionadas(adicionadas: List<AtividadeAdicionada>) {
    if (adicionadas.isEmpty()) {
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (atividade in adicionadas) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_presente),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = pluralStringResource(
                        R.plurals.chamada_pendencias_adicionada,
                        atividade.quantidadeDeAlunos,
                        atividade.descricao,
                        atividade.quantidadeDeAlunos,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
