package br.com.ricardo.diariodeclasse.notificacao

import android.content.Intent

/**
 * Tela que o app abre quando a professora toca numa notificação.
 * Viaja dentro do `Intent` como "extras" (pares chave/valor), porque é só
 * isso que o sistema guarda e entrega de volta ao app.
 */
sealed interface DestinoDaNotificacao {
    data class PendenciasDaTurma(val turmaId: String) : DestinoDaNotificacao

    data object Lembretes : DestinoDaNotificacao
}

private const val CHAVE_DESTINO = "destino_da_notificacao"
private const val CHAVE_TURMA_ID = "turma_id"
private const val DESTINO_PENDENCIAS = "pendencias"
private const val DESTINO_LEMBRETES = "lembretes"

fun guardarDestinoNoIntent(intent: Intent, destino: DestinoDaNotificacao) {
    when (destino) {
        is DestinoDaNotificacao.PendenciasDaTurma -> {
            intent.putExtra(CHAVE_DESTINO, DESTINO_PENDENCIAS)
            intent.putExtra(CHAVE_TURMA_ID, destino.turmaId)
        }

        is DestinoDaNotificacao.Lembretes -> {
            intent.putExtra(CHAVE_DESTINO, DESTINO_LEMBRETES)
        }
    }
}

/** `null` quando o app foi aberto de outro jeito (ícone, lista de recentes). */
fun lerDestinoDoIntent(intent: Intent): DestinoDaNotificacao? {
    // Reaberto pela lista de apps recentes, o Android entrega de novo o Intent
    // original: sem esta checagem, a notificação de dias atrás seria reaplicada.
    val veioDosRecentes: Boolean = (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0
    if (veioDosRecentes) {
        return null
    }

    val destino: String? = intent.getStringExtra(CHAVE_DESTINO)
    if (destino == DESTINO_LEMBRETES) {
        return DestinoDaNotificacao.Lembretes
    }
    if (destino == DESTINO_PENDENCIAS) {
        val turmaId: String? = intent.getStringExtra(CHAVE_TURMA_ID)
        if (turmaId != null) {
            return DestinoDaNotificacao.PendenciasDaTurma(turmaId)
        }
    }
    return null
}
