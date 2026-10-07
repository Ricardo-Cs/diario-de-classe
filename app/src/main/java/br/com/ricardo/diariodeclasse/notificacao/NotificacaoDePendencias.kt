package br.com.ricardo.diariodeclasse.notificacao

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import br.com.ricardo.diariodeclasse.R
import br.com.ricardo.diariodeclasse.data.local.entity.PendenciaParaLembrete
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Monta e mostra a notificação diária "N pendências para hoje".
 *
 * Desde o Android 8, toda notificação pertence a um canal. O canal aparece nas
 * configurações do app e a professora pode silenciá-lo sem desligar o resto.
 */
class NotificacaoDePendencias @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /** Criar um canal que já existe não faz nada, então é seguro chamar a cada abertura do app. */
    fun criarCanal() {
        val canal = NotificationChannel(
            ID_DO_CANAL,
            context.getString(R.string.notificacao_canal_nome),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        canal.description = context.getString(R.string.notificacao_canal_descricao)

        val gerenciador: NotificationManager = context.getSystemService(NotificationManager::class.java)
        gerenciador.createNotificationChannel(canal)
    }

    fun temPermissao(): Boolean {
        return temPermissaoParaNotificar(context)
    }

    fun mostrar(pendencias: List<PendenciaParaLembrete>) {
        if (pendencias.isEmpty()) {
            return
        }
        if (!temPermissao()) {
            return
        }

        val titulo: String = context.resources.getQuantityString(
            R.plurals.notificacao_titulo,
            pendencias.size,
            pendencias.size,
        )
        val notificacao = NotificationCompat.Builder(context, ID_DO_CANAL)
            .setSmallIcon(R.drawable.ic_diario)
            .setContentTitle(titulo)
            .setContentText(textoDaLinha(pendencias.first()))
            .setStyle(montarListaExpandida(pendencias))
            .setContentIntent(criarAcaoDeAbrirApp(context))
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(ID_DA_NOTIFICACAO, notificacao)
    }

    /** Ao expandir a notificação, aparece uma linha por pendência (até um limite). */
    private fun montarListaExpandida(pendencias: List<PendenciaParaLembrete>): NotificationCompat.InboxStyle {
        val lista = NotificationCompat.InboxStyle()
        var mostradas = 0
        for (pendencia in pendencias) {
            if (mostradas == MAXIMO_DE_LINHAS) {
                break
            }
            lista.addLine(textoDaLinha(pendencia))
            mostradas = mostradas + 1
        }

        val restantes: Int = pendencias.size - mostradas
        if (restantes > 0) {
            lista.setSummaryText(context.getString(R.string.notificacao_e_mais, restantes))
        }
        return lista
    }

    private fun textoDaLinha(pendencia: PendenciaParaLembrete): String {
        return context.getString(R.string.notificacao_linha, pendencia.nomeDoAluno, pendencia.descricao)
    }

    companion object {
        private const val ID_DO_CANAL = "lembretes_pendencias"

        /** Sempre o mesmo id: o aviso de hoje substitui o de ontem, se ainda estiver lá. */
        private const val ID_DA_NOTIFICACAO = 1

        private const val MAXIMO_DE_LINHAS = 5
    }
}
