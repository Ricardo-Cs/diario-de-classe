package br.com.ricardo.diariodeclasse

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import br.com.ricardo.diariodeclasse.notificacao.DestinoDaNotificacao
import br.com.ricardo.diariodeclasse.notificacao.lerDestinoDoIntent
import br.com.ricardo.diariodeclasse.ui.navigation.AppNavHost
import br.com.ricardo.diariodeclasse.ui.theme.DiarioDeClasseTheme
import dagger.hilt.android.AndroidEntryPoint

/** `@AndroidEntryPoint` permite ao Hilt injetar dependências (e ViewModels) nesta Activity. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /**
     * Abre a janela do sistema "Permitir notificações?". Precisa ser registrado
     * antes de a Activity aparecer na tela, por isso é um campo da classe.
     * Se ela negar, o app continua funcionando; só não mostra o aviso diário.
     */
    private val pedidoDePermissaoDeNotificacao: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _: Boolean -> }

    /**
     * Tela pedida pela notificação tocada, à espera de o `AppNavHost` navegar até ela.
     * É um estado do Compose: quando muda, a interface reage, como um `useState`.
     */
    private val destinoDaNotificacao: MutableState<DestinoDaNotificacao?> = mutableStateOf(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // `savedInstanceState == null` = o app acabou de abrir (e não foi só o
        // celular girando a tela), para não pedir de novo a cada recriação.
        if (savedInstanceState == null) {
            pedirPermissaoDeNotificacaoSeNecessario()
            destinoDaNotificacao.value = lerDestinoDoIntent(intent)
        }

        setContent {
            DiarioDeClasseTheme {
                AppNavHost(
                    destinoDaNotificacao = destinoDaNotificacao.value,
                    aoAbrirDestinoDaNotificacao = { destinoDaNotificacao.value = null },
                )
            }
        }
    }

    /** Chamado no lugar do `onCreate` quando ela toca na notificação com o app já aberto. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        destinoDaNotificacao.value = lerDestinoDoIntent(intent)
    }

    /**
     * Só existe no Android 13+. Se ela negar duas vezes, o próprio Android para
     * de mostrar a janela; aí só dá para ativar pelas configurações do celular.
     */
    private fun pedirPermissaoDeNotificacaoSeNecessario() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }
        val permissao: Int = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
        if (permissao == PackageManager.PERMISSION_GRANTED) {
            return
        }
        pedidoDePermissaoDeNotificacao.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
