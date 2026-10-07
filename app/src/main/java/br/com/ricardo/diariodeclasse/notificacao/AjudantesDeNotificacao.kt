package br.com.ricardo.diariodeclasse.notificacao

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import br.com.ricardo.diariodeclasse.MainActivity

/*
 * Partes comuns às notificações do app (pendências e lembretes).
 */

/** No Android 13+ a notificação só aparece se a professora tiver permitido. */
fun temPermissaoParaNotificar(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return true
    }
    val permissao: Int = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
    return permissao == PackageManager.PERMISSION_GRANTED
}

/**
 * Abre o app como se fosse pelo ícone: se ele já estiver aberto, volta para
 * onde a professora estava em vez de recomeçar do zero.
 *
 * `PendingIntent` é uma "intenção guardada" que o sistema executa depois em
 * nome do app (quando ela tocar na notificação). `FLAG_IMMUTABLE` é exigido
 * desde o Android 12 e impede que outros apps alterem essa intenção.
 */
fun criarAcaoDeAbrirApp(context: Context): PendingIntent {
    val abrirApp = Intent(context, MainActivity::class.java)
    abrirApp.action = Intent.ACTION_MAIN
    abrirApp.addCategory(Intent.CATEGORY_LAUNCHER)
    abrirApp.flags = Intent.FLAG_ACTIVITY_NEW_TASK

    return PendingIntent.getActivity(context, 0, abrirApp, PendingIntent.FLAG_IMMUTABLE)
}
