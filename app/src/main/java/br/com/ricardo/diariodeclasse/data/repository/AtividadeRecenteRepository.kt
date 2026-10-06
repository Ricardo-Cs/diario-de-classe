package br.com.ricardo.diariodeclasse.data.repository

import br.com.ricardo.diariodeclasse.data.local.dao.AtividadeRecenteDao
import br.com.ricardo.diariodeclasse.data.local.dao.LinhaDeAtividade
import br.com.ricardo.diariodeclasse.data.local.entity.StatusPendencia
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDateTime
import javax.inject.Inject

/** Um registro recente da professora, já no formato que a tela entende. */
sealed interface AtividadeRecente {
    val momento: LocalDateTime

    data class Chamada(
        override val momento: LocalDateTime,
        val editada: Boolean,
        val ausentes: Int,
    ) : AtividadeRecente

    data class PendenciaRegistrada(
        override val momento: LocalDateTime,
        val editada: Boolean,
        val nomeDoAluno: String,
        val descricao: String,
    ) : AtividadeRecente

    data class PendenciaEntregue(
        override val momento: LocalDateTime,
        val nomeDoAluno: String,
        val descricao: String,
    ) : AtividadeRecente

    data class Anotacao(
        override val momento: LocalDateTime,
        val editada: Boolean,
        val nomeDoAluno: String,
        val texto: String,
    ) : AtividadeRecente
}

interface AtividadeRecenteRepository {
    /** As últimas ações na turma, da mais recente para a mais antiga. */
    fun observarDaTurma(turmaId: String, limite: Int): Flow<List<AtividadeRecente>>
}

class AtividadeRecenteRepositoryImpl @Inject constructor(
    private val dao: AtividadeRecenteDao,
    private val clock: Clock,
) : AtividadeRecenteRepository {

    override fun observarDaTurma(turmaId: String, limite: Int): Flow<List<AtividadeRecente>> {
        return dao.observarDaTurma(turmaId, limite).map { linhas -> converterLinhas(linhas) }
    }

    private fun converterLinhas(linhas: List<LinhaDeAtividade>): List<AtividadeRecente> {
        val atividades = mutableListOf<AtividadeRecente>()
        for (linha in linhas) {
            val atividade: AtividadeRecente? = converter(linha)
            if (atividade != null) {
                atividades.add(atividade)
            }
        }
        return atividades
    }

    /** "Editada" = alterada depois de criada. Linhas com origem desconhecida são ignoradas. */
    private fun converter(linha: LinhaDeAtividade): AtividadeRecente? {
        val momento: LocalDateTime = LocalDateTime.ofInstant(linha.momento, clock.zone)
        val editada: Boolean = linha.momento.isAfter(linha.criadoEm)

        when (linha.origem) {
            ORIGEM_CHAMADA -> {
                return AtividadeRecente.Chamada(
                    momento = momento,
                    editada = editada,
                    ausentes = linha.ausentes ?: 0,
                )
            }

            ORIGEM_PENDENCIA -> {
                val nomeDoAluno: String = linha.nomeDoAluno ?: ""
                val descricao: String = linha.texto ?: ""
                if (linha.status == StatusPendencia.ENTREGUE) {
                    return AtividadeRecente.PendenciaEntregue(momento, nomeDoAluno, descricao)
                }
                return AtividadeRecente.PendenciaRegistrada(momento, editada, nomeDoAluno, descricao)
            }

            ORIGEM_ANOTACAO -> {
                return AtividadeRecente.Anotacao(
                    momento = momento,
                    editada = editada,
                    nomeDoAluno = linha.nomeDoAluno ?: "",
                    texto = linha.texto ?: "",
                )
            }
        }
        return null
    }

    /** Os mesmos textos usados na consulta do [AtividadeRecenteDao]. */
    companion object {
        const val ORIGEM_CHAMADA = "CHAMADA"
        const val ORIGEM_PENDENCIA = "PENDENCIA"
        const val ORIGEM_ANOTACAO = "ANOTACAO"
    }
}
