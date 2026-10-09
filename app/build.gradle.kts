import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

/**
 * Dados da chave de assinatura do APK de release. Ficam no `local.properties`
 * (fora do git), nunca neste arquivo. Sem eles, o build de release sai sem
 * assinatura e o build de debug continua funcionando normalmente.
 */
val propriedadesLocais = Properties()
val arquivoDePropriedadesLocais: File = rootProject.file("local.properties")
if (arquivoDePropriedadesLocais.exists()) {
    val entrada = FileInputStream(arquivoDePropriedadesLocais)
    propriedadesLocais.load(entrada)
    entrada.close()
}
val caminhoDaChave: String? = propriedadesLocais.getProperty("assinatura.arquivo")

android {
    namespace = "br.com.ricardo.diariodeclasse"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "br.com.ricardo.diariodeclasse"
        minSdk = 26
        targetSdk = 37
        versionCode = 2
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (caminhoDaChave != null) {
                storeFile = file(caminhoDaChave)
                storePassword = propriedadesLocais.getProperty("assinatura.senhaDoArquivo")
                keyAlias = propriedadesLocais.getProperty("assinatura.apelido")
                keyPassword = propriedadesLocais.getProperty("assinatura.senhaDaChave")
            }
        }
    }

    buildTypes {
        release {
            if (caminhoDaChave != null) {
                signingConfig = signingConfigs.getByName("release")
            }
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

ksp {
    // Room grava o schema de cada versão do banco em JSON (versionar no git).
    // Serve de base para escrever e testar migrações sem perder dados.
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)

    // Navegação (rotas tipadas com @Serializable)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore (preferências simples, como a turma ativa)
    implementation(libs.androidx.datastore.preferences)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Coil: carrega as fotos do diário na tela, já reduzidas ao tamanho exibido
    implementation(libs.coil.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.room.testing)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
