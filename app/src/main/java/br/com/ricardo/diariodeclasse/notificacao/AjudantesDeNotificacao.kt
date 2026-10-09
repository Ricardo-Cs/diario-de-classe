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
 * Abre o app direto na tela de [destino], ou no Início quando ele é `null`.
 *
 * `PendingIntent` é uma "intenção guardada" que o sistema executa depois em
 * nome do app (quando ela tocar na notificação). `FLAG_IMMUTABLE` é exigido
 * desde o Android 12 e impede que outros apps alterem essa intenção.
 *
 * - `CLEAR_TOP` + `SINGLE_TOP`: com o app já aberto, o Android reaproveita a
 *   Activity existente e entrega o Intent em `onNewIntent`, em vez de só trazer
 *   o app para a frente e ignorar o destino.
 * - [codigo] diferente por notificação: o Android considera iguais dois
 *   `PendingIntent` que só diferem nos extras, e um tomaria o lugar do outro.
 * - `FLAG_UPDATE_CURRENT`: a turma do aviso de hoje substitui a de ontem.
 */
fun criarAcaoDeAbrirApp(context: Context, codigo: Int, destino: DestinoDaNotificacao?): PendingIntent {
    val abrirApp = Intent(context, MainActivity::class.java)
    abrirApp.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
        Intent.FLAG_ACTIVITY_CLEAR_TOP or
        Intent.FLAG_ACTIVITY_SINGLE_TOP
    if (destino != null) {
        guardarDestinoNoIntent(abrirApp, destino)
    }

    val opcoes: Int = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    return PendingIntent.getActivity(context, codigo, abrirApp, opcoes)
}
