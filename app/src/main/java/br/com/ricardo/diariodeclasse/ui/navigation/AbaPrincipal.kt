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
    A_FAZER(AFazerGrafo, R.string.aba_a_fazer, R.drawable.ic_a_fazer),
    TURMA(TurmaGrafo, R.string.aba_turma, R.drawable.ic_turma),
    ACOMPANHAMENTO(AcompanhamentoGrafo, R.string.aba_acompanhar, R.drawable.ic_diario),

    /** Na barra, o ícone dá lugar à foto da professora (ver `BarraInferior`). */
    PERFIL(PerfilGrafo, R.string.aba_perfil, R.drawable.ic_perfil),
}
