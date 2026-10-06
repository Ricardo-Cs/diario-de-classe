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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import java.time.LocalDate

/**
 * Painel para criar uma pendência avulsa. Quando aberto a partir de um aluno
 * ([alunoInicial]), ele já vem escolhido e a professora só digita a atividade.
 *
 * `ModalBottomSheet` ainda é experimental no Material 3; o `@OptIn` fica só aqui.
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
