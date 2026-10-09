package br.com.ricardo.diariodeclasse.data.fotos

import android.media.ExifInterface
import org.junit.Assert.assertEquals
import org.junit.Test

class ReducaoDaFotoTest {

    @Test
    fun reducaoNaLeitura_maiorPotenciaDe2QueNaoDeixaMenorQueOLimite() {
        // 4000 / 2 = 2000 (>= 1600); 4000 / 4 = 1000 (< 1600): fica em 2.
        assertEquals(2, calcularReducaoNaLeitura(TamanhoDaFoto(4000, 3000), 1600))
        // 8000 / 4 = 2000; 8000 / 8 = 1000: fica em 4.
        assertEquals(4, calcularReducaoNaLeitura(TamanhoDaFoto(6000, 8000), 1600))
    }

    @Test
    fun reducaoNaLeitura_fotoPequenaNaoReduz() {
        assertEquals(1, calcularReducaoNaLeitura(TamanhoDaFoto(1200, 900), 1600))
        assertEquals(1, calcularReducaoNaLeitura(TamanhoDaFoto(3000, 2000), 1600))
    }

    @Test
    fun tamanhoFinal_mantemAProporcao() {
        assertEquals(TamanhoDaFoto(1600, 1200), tamanhoFinal(TamanhoDaFoto(2000, 1500), 1600))
        assertEquals(TamanhoDaFoto(900, 1600), tamanhoFinal(TamanhoDaFoto(1800, 3200), 1600))
    }

    @Test
    fun tamanhoFinal_naoAmpliaFotoPequena() {
        assertEquals(TamanhoDaFoto(800, 600), tamanhoFinal(TamanhoDaFoto(800, 600), 1600))
    }

    @Test
    fun rotacao_segueOrientacaoDoExif() {
        assertEquals(0, rotacaoDaOrientacao(ExifInterface.ORIENTATION_NORMAL))
        assertEquals(90, rotacaoDaOrientacao(ExifInterface.ORIENTATION_ROTATE_90))
        assertEquals(180, rotacaoDaOrientacao(ExifInterface.ORIENTATION_ROTATE_180))
        assertEquals(270, rotacaoDaOrientacao(ExifInterface.ORIENTATION_ROTATE_270))
        assertEquals(0, rotacaoDaOrientacao(ExifInterface.ORIENTATION_UNDEFINED))
    }
}
