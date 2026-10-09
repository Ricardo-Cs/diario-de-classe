package br.com.ricardo.diariodeclasse.data.fotos

import android.media.ExifInterface

/*
 * Cálculos para reduzir a foto antes de guardar. Uma foto de celular tem de 3 a 6 MB;
 * com o lado maior em 1600 px e JPEG de qualidade 85, fica com uns 300 a 500 KB e
 * continua nítida na tela. São funções puras (sem Android), testadas com testes comuns.
 */

/** Tamanho do lado maior da foto guardada, em pixels. */
const val LADO_MAXIMO_DA_FOTO = 1600

/** Qualidade do JPEG (0 a 100). Acima de 85 o arquivo cresce muito e a diferença não aparece. */
const val QUALIDADE_DO_JPEG = 85

data class TamanhoDaFoto(val largura: Int, val altura: Int)

/**
 * Quanto reduzir já na leitura do arquivo (`inSampleSize` do Android): lê só 1 de
 * cada N pixels, e N precisa ser potência de 2. Escolhe o maior N que ainda deixa
 * o lado maior com pelo menos [ladoMaximo]; o ajuste fino vem depois, em [tamanhoFinal].
 *
 * Sem isso, uma foto de 4000 x 3000 seria carregada inteira na memória (48 MB).
 */
fun calcularReducaoNaLeitura(tamanhoOriginal: TamanhoDaFoto, ladoMaximo: Int): Int {
    val ladoMaior: Int = maxOf(tamanhoOriginal.largura, tamanhoOriginal.altura)
    var reducao = 1
    while (ladoMaior / (reducao * 2) >= ladoMaximo) {
        reducao = reducao * 2
    }
    return reducao
}

/** Tamanho final mantendo a proporção. Fotos já menores que [ladoMaximo] não são ampliadas. */
fun tamanhoFinal(tamanhoLido: TamanhoDaFoto, ladoMaximo: Int): TamanhoDaFoto {
    val ladoMaior: Int = maxOf(tamanhoLido.largura, tamanhoLido.altura)
    if (ladoMaior <= ladoMaximo) {
        return tamanhoLido
    }
    val escala: Double = ladoMaximo.toDouble() / ladoMaior
    val largura: Int = Math.round(tamanhoLido.largura * escala).toInt()
    val altura: Int = Math.round(tamanhoLido.altura * escala).toInt()
    return TamanhoDaFoto(largura = maxOf(largura, 1), altura = maxOf(altura, 1))
}

/**
 * A câmera grava a foto sempre "deitada" e anota no EXIF (os metadados do JPEG)
 * como o celular estava. Ao reduzir, esses metadados se perdem, então giramos
 * os pixels de verdade. Devolve o ângulo em graus, no sentido horário.
 */
fun rotacaoDaOrientacao(orientacaoDoExif: Int): Int {
    return when (orientacaoDoExif) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90
        ExifInterface.ORIENTATION_ROTATE_180 -> 180
        ExifInterface.ORIENTATION_ROTATE_270 -> 270
        else -> 0
    }
}
