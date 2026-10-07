package br.com.ricardo.diariodeclasse.notificacao

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import br.com.ricardo.diariodeclasse.R
import br.com.ricardo.diariodeclasse.data.local.entity.Lembrete
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Notificação diária dos lembretes da professora: os do dia e os atrasados.
 *
 * Tem canal próprio, separado do das pendências: nas configurações do Android
 * ela pode silenciar um sem desligar o outro.
 */
class NotificacaoDeLembretes @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /** Criar um canal que já existe não faz nada, então é seguro chamar a cada abertura do app. */
    fun criarCanal() {
        val canal = NotificationChannel(
            ID_DO_CANAL,
            context.getString(R.string.notificacao_lembretes_canal_nome),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        canal.description = context.getString(R.string.notificacao_lembretes_canal_descricao)

        val gerenciador: NotificationManager = context.getSystemService(NotificationManager::class.java)
        gerenciador.createNotificationChannel(canal)
    }

    fun mostrar(lembretes: List<Lembrete>, hoje: LocalDate) {
        if (lembretes.isEmpty()) {
            return
        }
        // A checagem explícita também é o que o lint pede antes de `notify`.
        if (!temPermissaoParaNotificar(context)) {
            return
        }

        val titulo: String = context.resources.getQuantityString(
            R.plurals.notificacao_lembretes_titulo,
            lembretes.size,
            lembretes.size,
        )
        val lista = NotificationCompat.InboxStyle()
        for (lembrete in lembretes) {
            lista.addLine(textoDaLinha(lembrete, hoje))
        }

        val notificacao = NotificationCompat.Builder(context, ID_DO_CANAL)
            .setSmallIcon(R.drawable.ic_diario)
            .setContentTitle(titulo)
            .setContentText(textoDaLinha(lembretes.first(), hoje))
            .setStyle(lista)
            .setContentIntent(criarAcaoDeAbrirApp(context))
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(ID_DA_NOTIFICACAO, notificacao)
        } catch (erro: SecurityException) {
            // A permissão pode ser retirada entre a checagem acima e este ponto.
        }
    }

    /** "Entregar portfólio · hoje" ou "Plano de ação · atrasado desde 02/10". */
    private fun textoDaLinha(lembrete: Lembrete, hoje: LocalDate): String {
        if (lembrete.data.isBefore(hoje)) {
            val dataCurta: String = lembrete.data.format(DateTimeFormatter.ofPattern("dd/MM"))
            return context.getString(R.string.notificacao_lembrete_atrasado, lembrete.descricao, dataCurta)
        }
        return context.getString(R.string.notificacao_lembrete_hoje, lembrete.descricao)
    }

    companion object {
        private const val ID_DO_CANAL = "lembretes_da_professora"

        /** Diferente do id das pendências (1): as duas notificações convivem. */
        private const val ID_DA_NOTIFICACAO = 2
    }
}
