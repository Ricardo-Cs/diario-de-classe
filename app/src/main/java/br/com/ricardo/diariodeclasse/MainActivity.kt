package br.com.ricardo.diariodeclasse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import br.com.ricardo.diariodeclasse.ui.navigation.AppNavHost
import br.com.ricardo.diariodeclasse.ui.theme.DiarioDeClasseTheme
import dagger.hilt.android.AndroidEntryPoint

/** `@AndroidEntryPoint` permite ao Hilt injetar dependências (e ViewModels) nesta Activity. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DiarioDeClasseTheme {
                AppNavHost()
            }
        }
    }
}
