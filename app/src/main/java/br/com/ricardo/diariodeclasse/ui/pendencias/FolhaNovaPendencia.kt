package br.com.ricardo.diariodeclasse.ui.pendencias

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import br.com.ricardo.diariodeclasse.R
import br.com.ricardo.diariodeclasse.data.local.entity.Aluno
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Painel para criar uma pendência avulsa. Quando aberto a partir de um aluno
 * ([alunoInicial]), ele já vem escolhido e a professora só digita a atividade.
 *
 * `ModalBottomSheet` e `DatePicker` ainda são experimentais no Material 3; o `@OptIn` fica só aqui.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolhaNovaPendencia(
    alunos: List<Aluno>,
    alunoInicial: Aluno?,
    hoje: LocalDate,
    aoSalvar: (alunoId: String, descricao: String, dataLembrete: LocalDate) -> Unit,
    aoFechar: () -> Unit,
) {
    val alunoEscolhido: MutableState<Aluno?> = remember { mutableStateOf(alunoInicial) }
    val descricao: MutableState<String> = remember { mutableStateOf("") }
    val dataLembrete: MutableState<LocalDate> = remember { mutableStateOf(hoje) }

    val aluno: Aluno? = alunoEscolhido.value
    val podeSalvar: Boolean = aluno != null && descricao.value.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = aoFechar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                .imePadding(),
        ) {
            Text(
                text = stringResource(R.string.pendencias_nova),
                style = MaterialTheme.typography.titleMedium,
            )

            SeletorDeAluno(
                alunos = alunos,
                alunoEscolhido = aluno,
                aoEscolher = { escolhido -> alunoEscolhido.value = escolhido },
            )

            OutlinedTextField(
                value = descricao.value,
                onValueChange = { texto -> descricao.value = texto },
                label = { Text(stringResource(R.string.pendencia_campo_descricao)) },
                placeholder = { Text(stringResource(R.string.pendencia_campo_descricao_exemplo)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            EscolhaDoLembrete(
                hoje = hoje,
                dataEscolhida = dataLembrete.value,
                aoEscolher = { data -> dataLembrete.value = data },
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(Modifier.weight(1f))
                Button(
                    enabled = podeSalvar,
                    onClick = {
                        if (aluno != null) {
                            aoSalvar(aluno.id, descricao.value, dataLembrete.value)
                        }
                    },
                ) {
                    Text(stringResource(R.string.salvar))
                }
            }
        }
    }
}

/** Mesmo padrão do seletor de turma do Início: botão + menu suspenso. */
@Composable
private fun SeletorDeAluno(
    alunos: List<Aluno>,
    alunoEscolhido: Aluno?,
    aoEscolher: (aluno: Aluno) -> Unit,
) {
    val menuAberto: MutableState<Boolean> = remember { mutableStateOf(false) }

    val texto: String
    if (alunoEscolhido == null) {
        texto = stringResource(R.string.pendencia_escolher_aluno)
    } else {
        texto = alunoEscolhido.nome
    }

    Box {
        OutlinedButton(
            onClick = { menuAberto.value = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = texto, modifier = Modifier.weight(1f))
            Icon(painterResource(R.drawable.ic_expandir), contentDescription = null)
        }

        DropdownMenu(
            expanded = menuAberto.value,
            onDismissRequest = { menuAberto.value = false },
        ) {
            for (aluno in alunos) {
                DropdownMenuItem(
                    text = { Text(aluno.nome) },
                    onClick = {
                        aoEscolher(aluno)
                        menuAberto.value = false
                    },
                )
            }
        }
    }
}

/** Atalhos "Hoje" e "Amanhã" cobrem a maioria dos casos; "Outra data" abre o calendário. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EscolhaDoLembrete(
    hoje: LocalDate,
    dataEscolhida: LocalDate,
    aoEscolher: (data: LocalDate) -> Unit,
) {
    val amanha: LocalDate = hoje.plusDays(1)
    val ehHoje: Boolean = dataEscolhida == hoje
    val ehAmanha: Boolean = dataEscolhida == amanha
    val ehOutraData: Boolean = !ehHoje && !ehAmanha
    val calendarioAberto: MutableState<Boolean> = remember { mutableStateOf(false) }

    val textoOutraData: String
    if (ehOutraData) {
        textoOutraData = dataEscolhida.format(DateTimeFormatter.ofPattern("dd/MM"))
    } else {
        textoOutraData = stringResource(R.string.pendencia_outra_data)
    }

    Column {
        Text(
            text = stringResource(R.string.pendencia_lembrar),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = ehHoje,
                onClick = { aoEscolher(hoje) },
                label = { Text(stringResource(R.string.pendencia_hoje)) },
            )
            FilterChip(
                selected = ehAmanha,
                onClick = { aoEscolher(amanha) },
                label = { Text(stringResource(R.string.pendencia_amanha)) },
            )
            FilterChip(
                selected = ehOutraData,
                onClick = { calendarioAberto.value = true },
                label = { Text(textoOutraData) },
            )
        }
    }

    if (calendarioAberto.value) {
        DialogoCalendario(
            dataInicial = dataEscolhida,
            aoEscolher = { data ->
                aoEscolher(data)
                calendarioAberto.value = false
            },
            aoCancelar = { calendarioAberto.value = false },
        )
    }
}

/**
 * O `DatePicker` trabalha com milissegundos em UTC (meia-noite do dia escolhido),
 * por isso as conversões usam `ZoneOffset.UTC` e não o fuso do aparelho.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogoCalendario(
    dataInicial: LocalDate,
    aoEscolher: (data: LocalDate) -> Unit,
    aoCancelar: () -> Unit,
) {
    val milissegundosIniciais: Long = dataInicial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val estadoDoCalendario: DatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = milissegundosIniciais,
    )

    DatePickerDialog(
        onDismissRequest = aoCancelar,
        confirmButton = {
            TextButton(
                onClick = {
                    val milissegundos: Long? = estadoDoCalendario.selectedDateMillis
                    if (milissegundos != null) {
                        val data: LocalDate = Instant.ofEpochMilli(milissegundos).atZone(ZoneOffset.UTC).toLocalDate()
                        aoEscolher(data)
                    }
                },
            ) {
                Text(stringResource(R.string.concluir))
            }
        },
        dismissButton = {
            TextButton(onClick = aoCancelar) {
                Text(stringResource(R.string.cancelar))
            }
        },
    ) {
        DatePicker(state = estadoDoCalendario)
    }
}
