package br.com.ricardo.diariodeclasse.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Papéis das cores da paleta:
 * - primary (Molten Lava): ações principais e botões de destaque (inclusive o container,
 *   usado pelos botões flutuantes, para a cor da marca aparecer igual nos dois temas).
 * - secondary (Deep Space Blue): elementos de apoio.
 * - tertiary (ocre): ausências (ver [ausencia]).
 * - error (Brick Red): atrasos e ações destrutivas.
 * - background/surface: creme "papel" no tema claro, com cartões brancos
 *   (surfaceContainerLowest) por cima; azul-marinho com texto Papaya Whip no escuro.
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

    tertiary = Ocre,
    onTertiary = Branco,
    tertiaryContainer = OcreClaro,
    onTertiaryContainer = OcreEscuro,

    error = BrickRed,
    onError = Branco,
    errorContainer = VermelhoClaro,
    onErrorContainer = MoltenLava,

    background = CremePapel,
    onBackground = CinzaTexto,
    surface = CremePapel,
    onSurface = CinzaTexto,
    surfaceVariant = CremeForte,
    onSurfaceVariant = CinzaTextoSuave,
    surfaceContainerLowest = Branco,
    surfaceContainerLow = CremeMaisClaro,
    surfaceContainer = CremeClaro,
    surfaceContainerHigh = CremeMedio,
    surfaceContainerHighest = CremeForte,
    outline = CinzaContorno,
    outlineVariant = CremeContornoSuave,
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

    tertiary = OcreSuave,
    onTertiary = OcreTextoEscuro,
    tertiaryContainer = OcreProfundo,
    onTertiaryContainer = OcreClaro,

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
 * O Material 3 não tem um papel "ausência"; usamos o `tertiary`, que nenhuma outra
 * tela usa. Esta propriedade dá um nome claro a esse uso:
 * `MaterialTheme.colorScheme.ausencia` em vez de um `tertiary` sem contexto.
 *
 * É uma "extension property": acrescenta uma propriedade a uma classe que não é
 * nossa (`ColorScheme`), parecido com adicionar um getter de fora da classe.
 */
val ColorScheme.ausencia: Color
    get() = tertiary

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
