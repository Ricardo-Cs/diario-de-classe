package br.com.ricardo.diariodeclasse.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import br.com.ricardo.diariodeclasse.R

/*
 * Duas fontes, empacotadas em `res/font` (nada é baixado da internet):
 * - Fraunces: serifa suave, com cara de caderno, só nos títulos.
 * - Atkinson Hyperlegible Next: criada para máxima legibilidade, no resto do texto.
 * Licenças (SIL OFL) em `assets/licencas/`.
 *
 * As duas são fontes "variáveis": um único arquivo contém todos os pesos, e
 * escolhemos o peso (e outros eixos) por `FontVariation.Settings`. Essa forma de
 * criar a `Font` ainda é marcada como experimental; o `@OptIn` fica só nas duas funções abaixo.
 */

/**
 * Eixos próprios da Fraunces:
 * - "opsz" (tamanho óptico): desenho pensado para títulos médios;
 * - "SOFT" (0 a 100): arredonda as serifas, deixando a letra mais amigável.
 */
@OptIn(ExperimentalTextApi::class)
private fun fonteFraunces(peso: FontWeight): Font {
    return Font(
        resId = R.font.fraunces,
        weight = peso,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(peso.weight),
            FontVariation.Setting("opsz", 48f),
            FontVariation.Setting("SOFT", 50f),
        ),
    )
}

@OptIn(ExperimentalTextApi::class)
private fun fonteAtkinson(peso: FontWeight): Font {
    return Font(
        resId = R.font.atkinson_hyperlegible_next,
        weight = peso,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(peso.weight),
        ),
    )
}

/** Cada `Font` da família responde por um peso; o Compose escolhe pelo `fontWeight` do texto. */
private val Fraunces: FontFamily = FontFamily(
    fonteFraunces(FontWeight.Normal),
    fonteFraunces(FontWeight.SemiBold),
)

private val AtkinsonHyperlegible: FontFamily = FontFamily(
    fonteAtkinson(FontWeight.Normal),
    fonteAtkinson(FontWeight.Medium),
    fonteAtkinson(FontWeight.SemiBold),
    fonteAtkinson(FontWeight.Bold),
)

/** Tamanhos, alturas de linha e espaçamentos padrão do Material 3; trocamos só a fonte. */
private val PadraoDoMaterial: Typography = Typography()

/**
 * Títulos grandes (display, headline e titleLarge, que é o título da barra do topo)
 * usam a Fraunces. Títulos menores, corpo e rótulos usam a Atkinson, porque
 * aparecem em tamanhos pequenos, onde a legibilidade pesa mais que o estilo.
 */
val Typography: Typography = Typography(
    displayLarge = PadraoDoMaterial.displayLarge.copy(fontFamily = Fraunces),
    displayMedium = PadraoDoMaterial.displayMedium.copy(fontFamily = Fraunces),
    displaySmall = PadraoDoMaterial.displaySmall.copy(fontFamily = Fraunces),
    headlineLarge = PadraoDoMaterial.headlineLarge.copy(fontFamily = Fraunces, fontWeight = FontWeight.SemiBold),
    headlineMedium = PadraoDoMaterial.headlineMedium.copy(fontFamily = Fraunces, fontWeight = FontWeight.SemiBold),
    headlineSmall = PadraoDoMaterial.headlineSmall.copy(fontFamily = Fraunces, fontWeight = FontWeight.SemiBold),
    titleLarge = PadraoDoMaterial.titleLarge.copy(fontFamily = Fraunces, fontWeight = FontWeight.SemiBold),

    titleMedium = PadraoDoMaterial.titleMedium.copy(fontFamily = AtkinsonHyperlegible, fontWeight = FontWeight.SemiBold),
    titleSmall = PadraoDoMaterial.titleSmall.copy(fontFamily = AtkinsonHyperlegible, fontWeight = FontWeight.SemiBold),
    bodyLarge = PadraoDoMaterial.bodyLarge.copy(fontFamily = AtkinsonHyperlegible),
    bodyMedium = PadraoDoMaterial.bodyMedium.copy(fontFamily = AtkinsonHyperlegible),
    bodySmall = PadraoDoMaterial.bodySmall.copy(fontFamily = AtkinsonHyperlegible),
    labelLarge = PadraoDoMaterial.labelLarge.copy(fontFamily = AtkinsonHyperlegible),
    labelMedium = PadraoDoMaterial.labelMedium.copy(fontFamily = AtkinsonHyperlegible),
    labelSmall = PadraoDoMaterial.labelSmall.copy(fontFamily = AtkinsonHyperlegible),
)
