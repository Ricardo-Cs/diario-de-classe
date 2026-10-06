package br.com.ricardo.diariodeclasse.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Papéis das cores da paleta:
 * - primary (Molten Lava): ações principais e botões de destaque (inclusive o container,
 *   usado pelos botões flutuantes, para a cor da marca aparecer igual nos dois temas).
 * - secondary (Deep Space Blue): elementos de apoio.
 * - tertiary (Steel Blue): destaques informativos.
 * - error (Brick Red): ações destrutivas e alertas.
 * - background/surface: branco no tema claro; azul-marinho com texto Papaya Whip no escuro.
 */
private val EsquemaClaro: ColorScheme = lightColorScheme(
    primary = MoltenLava,
    onPrimary = Branco,
    primaryContainer = MoltenLava,
    onPrimaryContainer = PapayaWhip,
    // Usado em elementos sobre fundo invertido, como o "Desfazer" dos avisos (snackbar).
    inversePrimary = LavaRosado,

    secondary = DeepSpaceBlue,
    onSecondary = Branco,
    secondaryContainer = AzulClaro,
    onSecondaryContainer = DeepSpaceBlue,

    tertiary = SteelBlue,
    onTertiary = DeepSpaceBlue,
    tertiaryContainer = AzulClaro,
    onTertiaryContainer = DeepSpaceBlue,

    error = BrickRed,
    onError = Branco,
    errorContainer = VermelhoClaro,
    onErrorContainer = MoltenLava,

    background = Branco,
    onBackground = CinzaTexto,
    surface = Branco,
    onSurface = CinzaTexto,
    surfaceVariant = CinzaForte,
    onSurfaceVariant = CinzaTextoSuave,
    surfaceContainerLowest = Branco,
    surfaceContainerLow = CinzaMaisClaro,
    surfaceContainer = CinzaClaro,
    surfaceContainerHigh = CinzaMedio,
    surfaceContainerHighest = CinzaForte,
    outline = CinzaContorno,
    outlineVariant = CinzaContornoSuave,
)

private val EsquemaEscuro: ColorScheme = darkColorScheme(
    primary = LavaRosado,
    onPrimary = MoltenLava,
    primaryContainer = MoltenLava,
    onPrimaryContainer = LavaClaro,
    inversePrimary = MoltenLava,

    secondary = AzulCeu,
    onSecondary = DeepSpaceBlue,
    secondaryContainer = AzulPetroleo,
    onSecondaryContainer = AzulClaro,

    tertiary = SteelBlue,
    onTertiary = DeepSpaceBlue,
    tertiaryContainer = AzulPetroleo,
    onTertiaryContainer = AzulClaro,

    error = VermelhoRosado,
    onError = MoltenLava,
    errorContainer = VermelhoProfundo,
    onErrorContainer = VermelhoClaro,

    background = MarinhoFundo,
    onBackground = PapayaWhip,
    surface = MarinhoFundo,
    onSurface = PapayaWhip,
    surfaceVariant = MarinhoVariante,
    onSurfaceVariant = BegeTextoSuave,
    surfaceContainerLowest = MarinhoMaisEscuro,
    surfaceContainerLow = MarinhoLeve,
    surfaceContainer = MarinhoMedio,
    surfaceContainerHigh = MarinhoForte,
    surfaceContainerHighest = MarinhoMaisForte,
    outline = CinzaAzulado,
    outlineVariant = MarinhoContorno,
)

/**
 * Não usamos a "cor dinâmica" do Android 12+ (cores tiradas do papel de parede),
 * para que o app tenha sempre a paleta própria.
 */
@Composable
fun DiarioDeClasseTheme(
    temaEscuro: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val esquemaDeCores: ColorScheme
    if (temaEscuro) {
        esquemaDeCores = EsquemaEscuro
    } else {
        esquemaDeCores = EsquemaClaro
    }

    MaterialTheme(
        colorScheme = esquemaDeCores,
        typography = Typography,
        content = content,
    )
}
