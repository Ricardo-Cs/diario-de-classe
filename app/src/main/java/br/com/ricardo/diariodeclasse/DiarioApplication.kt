package br.com.ricardo.diariodeclasse

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Ponto de entrada do processo. `@HiltAndroidApp` gera o container de dependências
 * que vive enquanto o app estiver aberto (equivalente ao ApplicationContext do Spring).
 * Precisa estar registrada em `android:name` no AndroidManifest.
 */
@HiltAndroidApp
class DiarioApplication : Application()
