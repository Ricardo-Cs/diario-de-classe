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
 * - Literata: serifa desenhada para leitura em tela (é a do Google Play Livros), só nos títulos.
 *   Substituiu a Fraunces, cujo "f" e "j" pareciam quebrados em telas de menor resolução.
 * - Atkinson Hyperlegible Next: criada para máxima legibilidade, no resto do texto.
 * Licenças (SIL OFL) em `assets/licencas/`.
 *
 * As duas são fontes "variáveis": um único arquivo contém todos os pesos, e
 * escolhemos o peso (e outros eixos) por `FontVariation.Settings`. Essa forma de
 * criar a `Font` ainda é marcada como experimental; o `@OptIn` fica só nas duas funções abaixo.
 */

/**
 * "opsz" (tamanho óptico, 7 a 72): a Literata ajusta o desenho ao tamanho da letra.
 * 24 corresponde aos títulos do app, que vão de 22 a 32sp.
 */
@OptIn(ExperimentalTextApi::class)
private fun fonteLiterata(peso: FontWeight): Font {
    return Font(
        resId = R.font.literata,
        weight = peso,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(peso.weight),
            FontVariation.Setting("opsz", 24f),
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
private val Literata: FontFamily = FontFamily(
    fonteLiterata(FontWeight.Normal),
    fonteLiterata(FontWeight.SemiBold),
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
 * usam a Literata. Títulos menores, corpo e rótulos usam a Atkinson, porque
 * aparecem em tamanhos pequenos, onde a legibilidade pesa mais que o estilo.
 */
val Typography: Typography = Typography(
    displayLarge = PadraoDoMaterial.displayLarge.copy(fontFamily = Literata),
    displayMedium = PadraoDoMaterial.displayMedium.copy(fontFamily = Literata),
    displaySmall = PadraoDoMaterial.displaySmall.copy(fontFamily = Literata),
    headlineLarge = PadraoDoMaterial.headlineLarge.copy(fontFamily = Literata, fontWeight = FontWeight.SemiBold),
    headlineMedium = PadraoDoMaterial.headlineMedium.copy(fontFamily = Literata, fontWeight = FontWeight.SemiBold),
    headlineSmall = PadraoDoMaterial.headlineSmall.copy(fontFamily = Literata, fontWeight = FontWeight.SemiBold),
    titleLarge = PadraoDoMaterial.titleLarge.copy(fontFamily = Literata, fontWeight = FontWeight.SemiBold),

    titleMedium = PadraoDoMaterial.titleMedium.copy(fontFamily = AtkinsonHyperlegible, fontWeight = FontWeight.SemiBold),
    titleSmall = PadraoDoMaterial.titleSmall.copy(fontFamily = AtkinsonHyperlegible, fontWeight = FontWeight.SemiBold),
    bodyLarge = PadraoDoMaterial.bodyLarge.copy(fontFamily = AtkinsonHyperlegible),
    bodyMedium = PadraoDoMaterial.bodyMedium.copy(fontFamily = AtkinsonHyperlegible),
    bodySmall = PadraoDoMaterial.bodySmall.copy(fontFamily = AtkinsonHyperlegible),
    labelLarge = PadraoDoMaterial.labelLarge.copy(fontFamily = AtkinsonHyperlegible),
    labelMedium = PadraoDoMaterial.labelMedium.copy(fontFamily = AtkinsonHyperlegible),
    labelSmall = PadraoDoMaterial.labelSmall.copy(fontFamily = AtkinsonHyperlegible),
)
