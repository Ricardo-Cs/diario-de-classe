package br.com.ricardo.diariodeclasse.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import br.com.ricardo.diariodeclasse.R

/**
 * Itens da barra inferior, na ordem em que aparecem.
 *
 * O Diário ainda não tem conteúdo: a aba fica fora da barra para a professora
 * não encontrar uma tela "Em construção". A rota e a tela continuam no projeto;
 * para mostrar a aba, basta voltar com a linha
 * `DIARIO(DiarioGrafo, R.string.aba_diario, R.drawable.ic_diario)` entre TURMA e MAIS.
 */
enum class AbaPrincipal(
    val grafo: Any,
    @StringRes val titulo: Int,
    @DrawableRes val icone: Int,
) {
    INICIO(InicioGrafo, R.string.aba_inicio, R.drawable.ic_inicio),
    TURMA(TurmaGrafo, R.string.aba_turma, R.drawable.ic_turma),
    MAIS(MaisGrafo, R.string.aba_mais, R.drawable.ic_mais),
}
