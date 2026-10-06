package br.com.ricardo.diariodeclasse

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import br.com.ricardo.diariodeclasse.notificacao.LembreteDePendenciasWorker
import br.com.ricardo.diariodeclasse.notificacao.NotificacaoDePendencias
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Ponto de entrada do processo. `@HiltAndroidApp` gera o container de dependências
 * que vive enquanto o app estiver aberto (equivalente ao ApplicationContext do Spring).
 * Precisa estar registrada em `android:name` no AndroidManifest.
 *
 * `Configuration.Provider` entrega ao WorkManager a fábrica do Hilt, para que os
 * workers recebam repositórios por injeção. Por isso o manifest desliga a
 * inicialização automática do WorkManager.
 */
@HiltAndroidApp
class DiarioApplication : Application(), Configuration.Provider {

    /** `lateinit var`: o Hilt preenche o campo dentro de `super.onCreate()`. */
    @Inject
    lateinit var fabricaDeWorkers: HiltWorkerFactory

    @Inject
    lateinit var notificacaoDePendencias: NotificacaoDePendencias

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(fabricaDeWorkers)
            .build()

    override fun onCreate() {
        super.onCreate()
        notificacaoDePendencias.criarCanal()
        LembreteDePendenciasWorker.agendar(this)
    }
}
