package br.com.ricardo.diariodeclasse.ui.fotos

import java.time.LocalDate

/** As fotos de um dia, como aparecem na linha do tempo. */
data class DiaDeFotos(
    val data: LocalDate,
    val fotos: List<FotoNaTela>,
)

/**
 * Junta as fotos por dia, mantendo a ordem recebida (o banco já devolve os dias
 * mais recentes primeiro e, dentro do dia, na ordem em que foram adicionadas).
 */
fun agruparPorDia(fotos: List<FotoNaTela>): List<DiaDeFotos> {
    val dias = mutableListOf<DiaDeFotos>()
    var dataAtual: LocalDate? = null
    var fotosDoDia = mutableListOf<FotoNaTela>()

    for (foto in fotos) {
        val data: LocalDate = foto.foto.data
        if (data != dataAtual) {
            if (dataAtual != null) {
                dias.add(DiaDeFotos(dataAtual, fotosDoDia))
            }
            dataAtual = data
            fotosDoDia = mutableListOf()
        }
        fotosDoDia.add(foto)
    }
    if (dataAtual != null) {
        dias.add(DiaDeFotos(dataAtual, fotosDoDia))
    }
    return dias
}
