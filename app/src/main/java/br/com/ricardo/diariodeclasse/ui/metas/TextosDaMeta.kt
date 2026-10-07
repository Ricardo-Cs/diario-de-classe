package br.com.ricardo.diariodeclasse.ui.metas

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import br.com.ricardo.diariodeclasse.R
import br.com.ricardo.diariodeclasse.data.local.entity.Meta
import br.com.ricardo.diariodeclasse.data.local.entity.NivelDaMetrica
import br.com.ricardo.diariodeclasse.ui.componentes.formatarDataCurta
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/*
 * Textos da meta usados no Diário e na tela da meta, para os dois lugares
 * falarem do mesmo jeito.
 */

/** "Chegaram a Alfabético: 4 de 11" (por métrica) ou "Atingiram: 12 de 25" (à mão). */
@Composable
fun textoDoProgresso(progresso: ProgressoDaMeta): String {
    val atingiram: Int = progresso.quantidadeNaSituacao(SituacaoNaMeta.ATINGIU)
    val nivelAlvo: NivelDaMetrica? = progresso.nivelAlvo
    if (nivelAlvo == null) {
        return stringResource(R.string.meta_atingiram, atingiram, progresso.total())
    }
    return stringResource(R.string.meta_chegaram_a, atingiram, progresso.total(), nivelAlvo.nome)
}

/** Fração para a barra de progresso (0 a 1). Meta sem alunos fica vazia. */
fun fracaoAtingida(progresso: ProgressoDaMeta): Float {
    val total: Int = progresso.total()
    if (total == 0) {
        return 0f
    }
    val atingiram: Int = progresso.quantidadeNaSituacao(SituacaoNaMeta.ATINGIU)
    return atingiram.toFloat() / total.toFloat()
}

/**
 * "Prazo: 06/11 · faltam 31 dias", "Prazo: hoje", "Prazo: 06/11 · venceu há 2 dias",
 * "Sem prazo" ou "Encerrada em 06/11".
 */
@Composable
fun textoDoPrazo(meta: Meta, hoje: LocalDate): String {
    val encerradaEm: LocalDate? = meta.encerradaEm
    if (encerradaEm != null) {
        return stringResource(R.string.meta_encerrada_em, formatarDataCurta(encerradaEm, hoje))
    }
    val dataDoPrazo: LocalDate? = meta.prazo
    if (dataDoPrazo == null) {
        return stringResource(R.string.meta_sem_prazo)
    }

    val prazo: String = formatarDataCurta(dataDoPrazo, hoje)
    val diasAtePrazo: Int = ChronoUnit.DAYS.between(hoje, dataDoPrazo).toInt()
    if (diasAtePrazo == 0) {
        return stringResource(R.string.meta_prazo_hoje)
    }
    if (diasAtePrazo > 0) {
        return pluralStringResource(R.plurals.meta_prazo_faltam, diasAtePrazo, prazo, diasAtePrazo)
    }
    val diasDeAtraso: Int = -diasAtePrazo
    return pluralStringResource(R.plurals.meta_prazo_vencido, diasDeAtraso, prazo, diasDeAtraso)
}

/** Prazo vencido e meta ainda aberta: o texto ganha destaque. */
fun prazoVenceu(meta: Meta, hoje: LocalDate): Boolean {
    val prazo: LocalDate? = meta.prazo
    if (prazo == null || meta.encerradaEm != null) {
        return false
    }
    return prazo.isBefore(hoje)
}
