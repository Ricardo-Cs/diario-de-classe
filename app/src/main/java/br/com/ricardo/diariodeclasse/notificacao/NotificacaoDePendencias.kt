package br.com.ricardo.diariodeclasse.notificacao

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import br.com.ricardo.diariodeclasse.MainActivity
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

    /** No Android 13+ a notificação só aparece se a professora tiver permitido. */
    fun temPermissao(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }
        val permissao: Int = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        return permissao == PackageManager.PERMISSION_GRANTED
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
            .setContentIntent(criarAcaoDeAbrirApp())
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

    /**
     * Abre o app como se fosse pelo ícone: se ele já estiver aberto, volta para
     * onde a professora estava em vez de recomeçar do zero.
     *
     * `PendingIntent` é uma "intenção guardada" que o sistema executa depois em
     * nome do app (quando ela tocar na notificação). `FLAG_IMMUTABLE` é exigido
     * desde o Android 12 e impede que outros apps alterem essa intenção.
     */
    private fun criarAcaoDeAbrirApp(): PendingIntent {
        val abrirApp = Intent(context, MainActivity::class.java)
        abrirApp.action = Intent.ACTION_MAIN
        abrirApp.addCategory(Intent.CATEGORY_LAUNCHER)
        abrirApp.flags = Intent.FLAG_ACTIVITY_NEW_TASK

        return PendingIntent.getActivity(context, 0, abrirApp, PendingIntent.FLAG_IMMUTABLE)
    }

    companion object {
        private const val ID_DO_CANAL = "lembretes_pendencias"

        /** Sempre o mesmo id: o aviso de hoje substitui o de ontem, se ainda estiver lá. */
        private const val ID_DA_NOTIFICACAO = 1

        private const val MAXIMO_DE_LINHAS = 5
    }
}
