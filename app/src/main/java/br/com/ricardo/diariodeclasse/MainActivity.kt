package br.com.ricardo.diariodeclasse

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // `savedInstanceState == null` = o app acabou de abrir (e não foi só o
        // celular girando a tela), para não pedir de novo a cada recriação.
        if (savedInstanceState == null) {
            pedirPermissaoDeNotificacaoSeNecessario()
        }

        setContent {
            DiarioDeClasseTheme {
                AppNavHost()
            }
        }
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
