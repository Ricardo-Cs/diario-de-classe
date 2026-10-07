package br.com.ricardo.diariodeclasse.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import br.com.ricardo.diariodeclasse.R

/** Itens da barra inferior, na ordem em que aparecem. */
enum class AbaPrincipal(
    val grafo: Any,
    @StringRes val titulo: Int,
    @DrawableRes val icone: Int,
) {
    INICIO(InicioGrafo, R.string.aba_inicio, R.drawable.ic_inicio),
    TURMA(TurmaGrafo, R.string.aba_turma, R.drawable.ic_turma),
    DIARIO(DiarioGrafo, R.string.aba_diario, R.drawable.ic_diario),
    MAIS(MaisGrafo, R.string.aba_mais, R.drawable.ic_mais),
}
