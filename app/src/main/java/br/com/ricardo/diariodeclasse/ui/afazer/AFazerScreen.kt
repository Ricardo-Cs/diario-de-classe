package br.com.ricardo.diariodeclasse.ui.afazer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import br.com.ricardo.diariodeclasse.R
import br.com.ricardo.diariodeclasse.ui.componentes.BarraSuperior
import br.com.ricardo.diariodeclasse.ui.lembretes.ListaDeLembretes
import br.com.ricardo.diariodeclasse.ui.pendencias.ListaDePendencias

/**
 * Aba "A fazer": o que está em aberto, dos alunos (pendências) e da professora
 * (lembretes). As duas são listas de tarefas com data; por isso ficam juntas.
 *
 * A seção escolhida vem de fora ([secao]) para que o Início e as notificações
 * possam abrir direto a lista certa.
 */
@Composable
fun AFazerScreen(
    secao: SecaoDoAFazer,
    aoTrocarSecao: (secao: SecaoDoAFazer) -> Unit,
    aoAbrirAluno: (alunoId: String) -> Unit,
) {
    Scaffold(
        topBar = { BarraSuperior(titulo = stringResource(R.string.aba_a_fazer)) },
    ) { espacamentoDasBarras ->

        // As listas têm o próprio Scaffold (botão flutuante e avisos). `consumeWindowInsets`
        // avisa a elas que a barra de status já foi tratada aqui, para não somarem o espaço de novo.
        Column(
            modifier = Modifier
                .padding(espacamentoDasBarras)
                .consumeWindowInsets(espacamentoDasBarras)
                .fillMaxSize(),
        ) {
            AbasDasSecoes(secao = secao, aoTrocarSecao = aoTrocarSecao)

            when (secao) {
                SecaoDoAFazer.PENDENCIAS_DOS_ALUNOS -> ListaDePendencias(aoAbrirAluno = aoAbrirAluno)
                SecaoDoAFazer.MEUS_LEMBRETES -> ListaDeLembretes()
            }
        }
    }
}

@Composable
private fun AbasDasSecoes(secao: SecaoDoAFazer, aoTrocarSecao: (secao: SecaoDoAFazer) -> Unit) {
    // `ordinal` é a posição do item no enum (0, 1...), que é o índice da aba.
    PrimaryTabRow(selectedTabIndex = secao.ordinal) {
        Tab(
            selected = secao == SecaoDoAFazer.PENDENCIAS_DOS_ALUNOS,
            onClick = { aoTrocarSecao(SecaoDoAFazer.PENDENCIAS_DOS_ALUNOS) },
            text = { Text(stringResource(R.string.a_fazer_secao_alunos)) },
        )
        Tab(
            selected = secao == SecaoDoAFazer.MEUS_LEMBRETES,
            onClick = { aoTrocarSecao(SecaoDoAFazer.MEUS_LEMBRETES) },
            text = { Text(stringResource(R.string.a_fazer_secao_lembretes)) },
        )
    }
}
