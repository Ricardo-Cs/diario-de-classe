package br.com.ricardo.diariodeclasse.data.repository

import br.com.ricardo.diariodeclasse.data.local.dao.MetricaDao
import br.com.ricardo.diariodeclasse.data.local.entity.Metrica
import br.com.ricardo.diariodeclasse.data.local.entity.NivelDaMetrica
import br.com.ricardo.diariodeclasse.data.local.entity.NivelRegistradoDoAluno
import br.com.ricardo.diariodeclasse.data.local.entity.ResultadoDaSondagem
import br.com.ricardo.diariodeclasse.data.local.entity.ResultadoDatado
import br.com.ricardo.diariodeclasse.data.local.entity.Sondagem
import br.com.ricardo.diariodeclasse.data.local.entity.SondagemResumida
import kotlinx.coroutines.flow.Flow
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

/** Um nível como está no formulário da métrica. `id == null` é um nível novo. */
data class NivelEditado(
    val id: String?,
    val nome: String,
)

/** O que a tela de sondagem envia para um aluno. `nivelId == null` = não avaliado. */
data class MarcacaoDeNivel(
    val alunoId: String,
    val nivelId: String?,
)

/** Sondagem de um dia com os resultados que valem (sem os excluídos). */
data class SondagemDoDia(
    val sondagem: Sondagem,
    val resultados: List<ResultadoDaSondagem>,
)

interface MetricaRepository {
    fun observarMetricasDaTurma(turmaId: String): Flow<List<Metrica>>
    fun observarMetrica(id: String): Flow<Metrica?>
    suspend fun buscarMetrica(id: String): Metrica?

    /** Níveis da escala, do mais inicial ao mais avançado. */
    fun observarNiveis(metricaId: String): Flow<List<NivelDaMetrica>>
    fun observarNiveisDaTurma(turmaId: String): Flow<List<NivelDaMetrica>>

    /** Níveis que não podem ser removidos: têm alunos avaliados ou são usados por metas. */
    suspend fun buscarIdsDeNiveisEmUso(metricaId: String): List<String>

    /**
     * Cria a métrica (`metricaId == null`) ou atualiza a existente. A ordem da
     * lista [niveis] vira a ordem da escala; níveis que saíram da lista são removidos.
     * Quem chama não deve remover níveis em uso (ver [buscarIdsDeNiveisEmUso]).
     */
    suspend fun salvarMetrica(metricaId: String?, turmaId: String, nome: String, niveis: List<NivelEditado>): Metrica
    suspend fun excluirMetrica(id: String)

    fun observarResultadosDaTurma(turmaId: String): Flow<List<ResultadoDatado>>
    fun observarResultadosDaMetrica(metricaId: String): Flow<List<ResultadoDatado>>
    fun observarNiveisDoAluno(alunoId: String): Flow<List<NivelRegistradoDoAluno>>

    /** Da mais recente para a mais antiga. */
    fun observarSondagens(metricaId: String): Flow<List<SondagemResumida>>

    /** `null` enquanto não há sondagem desta métrica no dia. */
    suspend fun buscarSondagem(metricaId: String, data: LocalDate): SondagemDoDia?

    /**
     * Cria a sondagem do dia ou atualiza a existente. Se nenhum aluno ficar
     * avaliado, a sondagem é excluída (não faz sentido uma sondagem vazia na lista).
     */
    suspend fun salvarSondagem(metricaId: String, data: LocalDate, marcacoes: List<MarcacaoDeNivel>)
}

class MetricaRepositoryImpl @Inject constructor(
    private val dao: MetricaDao,
    private val clock: Clock,
) : MetricaRepository {

    override fun observarMetricasDaTurma(turmaId: String): Flow<List<Metrica>> {
        return dao.observarDaTurma(turmaId)
    }

    override fun observarMetrica(id: String): Flow<Metrica?> {
        return dao.observarPorId(id)
    }

    override suspend fun buscarMetrica(id: String): Metrica? {
        return dao.buscarPorId(id)
    }

    override fun observarNiveis(metricaId: String): Flow<List<NivelDaMetrica>> {
        return dao.observarNiveis(metricaId)
    }

    override fun observarNiveisDaTurma(turmaId: String): Flow<List<NivelDaMetrica>> {
        return dao.observarNiveisDaTurma(turmaId)
    }

    override suspend fun buscarIdsDeNiveisEmUso(metricaId: String): List<String> {
        return dao.buscarIdsDeNiveisEmUso(metricaId)
    }

    override suspend fun salvarMetrica(
        metricaId: String?,
        turmaId: String,
        nome: String,
        niveis: List<NivelEditado>,
    ): Metrica {
        val agora: Instant = Instant.now(clock)

        var metricaExistente: Metrica? = null
        if (metricaId != null) {
            metricaExistente = dao.buscarPorId(metricaId)
        }

        val metrica: Metrica
        val niveisAntigos: List<NivelDaMetrica>
        if (metricaExistente == null) {
            metrica = Metrica(
                id = UUID.randomUUID().toString(),
                turmaId = turmaId,
                nome = nome,
                createdAt = agora,
                updatedAt = agora,
            )
            niveisAntigos = emptyList()
        } else {
            metrica = metricaExistente.copy(nome = nome, updatedAt = agora)
            niveisAntigos = dao.buscarTodosOsNiveis(metricaExistente.id)
        }

        val niveisParaGravar = mutableListOf<NivelDaMetrica>()
        for (posicao in niveis.indices) {
            val editado: NivelEditado = niveis[posicao]
            val nivel: NivelDaMetrica? = montarNivel(metrica.id, editado, posicao, niveisAntigos, agora)
            if (nivel != null) {
                niveisParaGravar.add(nivel)
            }
        }
        niveisParaGravar.addAll(marcarRemovidos(niveisAntigos, niveis, agora))

        dao.salvarMetricaComNiveis(metrica, niveisParaGravar)
        return metrica
    }

    /** Retorna `null` quando o nível não mudou, para o `updatedAt` só avançar no que foi alterado. */
    private fun montarNivel(
        metricaId: String,
        editado: NivelEditado,
        ordem: Int,
        niveisAntigos: List<NivelDaMetrica>,
        agora: Instant,
    ): NivelDaMetrica? {
        val antigo: NivelDaMetrica? = buscarNivelPorId(niveisAntigos, editado.id)
        if (antigo == null) {
            return NivelDaMetrica(
                id = UUID.randomUUID().toString(),
                metricaId = metricaId,
                nome = editado.nome,
                ordem = ordem,
                createdAt = agora,
                updatedAt = agora,
            )
        }

        val mudouNome: Boolean = antigo.nome != editado.nome
        val mudouOrdem: Boolean = antigo.ordem != ordem
        val estavaRemovido: Boolean = antigo.deletedAt != null
        if (!mudouNome && !mudouOrdem && !estavaRemovido) {
            return null
        }
        return antigo.copy(nome = editado.nome, ordem = ordem, deletedAt = null, updatedAt = agora)
    }

    /** Níveis que existiam e não estão mais na lista do formulário. */
    private fun marcarRemovidos(
        niveisAntigos: List<NivelDaMetrica>,
        niveisEditados: List<NivelEditado>,
        agora: Instant,
    ): List<NivelDaMetrica> {
        val idsQueContinuam = mutableSetOf<String>()
        for (editado in niveisEditados) {
            val id: String? = editado.id
            if (id != null) {
                idsQueContinuam.add(id)
            }
        }

        val removidos = mutableListOf<NivelDaMetrica>()
        for (antigo in niveisAntigos) {
            val jaEstavaRemovido: Boolean = antigo.deletedAt != null
            if (!jaEstavaRemovido && antigo.id !in idsQueContinuam) {
                removidos.add(antigo.copy(deletedAt = agora, updatedAt = agora))
            }
        }
        return removidos
    }

    private fun buscarNivelPorId(niveis: List<NivelDaMetrica>, id: String?): NivelDaMetrica? {
        if (id == null) {
            return null
        }
        for (nivel in niveis) {
            if (nivel.id == id) {
                return nivel
            }
        }
        return null
    }

    override suspend fun excluirMetrica(id: String) {
        dao.marcarComoExcluida(id, Instant.now(clock))
    }

    override fun observarResultadosDaTurma(turmaId: String): Flow<List<ResultadoDatado>> {
        return dao.observarResultadosDaTurma(turmaId)
    }

    override fun observarResultadosDaMetrica(metricaId: String): Flow<List<ResultadoDatado>> {
        return dao.observarResultadosDaMetrica(metricaId)
    }

    override fun observarNiveisDoAluno(alunoId: String): Flow<List<NivelRegistradoDoAluno>> {
        return dao.observarNiveisDoAluno(alunoId)
    }

    override fun observarSondagens(metricaId: String): Flow<List<SondagemResumida>> {
        return dao.observarSondagens(metricaId)
    }

    override suspend fun buscarSondagem(metricaId: String, data: LocalDate): SondagemDoDia? {
        val sondagem: Sondagem? = dao.buscarSondagem(metricaId, data)
        if (sondagem == null || sondagem.deletedAt != null) {
            return null
        }

        val resultadosQueValem = mutableListOf<ResultadoDaSondagem>()
        for (resultado in dao.buscarResultados(sondagem.id)) {
            if (resultado.deletedAt == null) {
                resultadosQueValem.add(resultado)
            }
        }
        return SondagemDoDia(sondagem, resultadosQueValem)
    }

    override suspend fun salvarSondagem(metricaId: String, data: LocalDate, marcacoes: List<MarcacaoDeNivel>) {
        val agora: Instant = Instant.now(clock)
        val sondagemExistente: Sondagem? = dao.buscarSondagem(metricaId, data)
        val alguemAvaliado: Boolean = temAlgumAlunoAvaliado(marcacoes)

        if (sondagemExistente == null && !alguemAvaliado) {
            return
        }

        val sondagem: Sondagem
        val resultadosAntigos: List<ResultadoDaSondagem>
        if (sondagemExistente == null) {
            sondagem = Sondagem(
                id = UUID.randomUUID().toString(),
                metricaId = metricaId,
                data = data,
                createdAt = agora,
                updatedAt = agora,
            )
            resultadosAntigos = emptyList()
        } else {
            val deletedAt: Instant? = if (alguemAvaliado) null else agora
            sondagem = sondagemExistente.copy(deletedAt = deletedAt, updatedAt = agora)
            resultadosAntigos = dao.buscarResultados(sondagemExistente.id)
        }

        val resultadosParaGravar = mutableListOf<ResultadoDaSondagem>()
        for (marcacao in marcacoes) {
            val anterior: ResultadoDaSondagem? = buscarResultadoDoAluno(resultadosAntigos, marcacao.alunoId)
            val resultado: ResultadoDaSondagem? = montarResultado(sondagem.id, marcacao, anterior, agora)
            if (resultado != null) {
                resultadosParaGravar.add(resultado)
            }
        }

        dao.salvarSondagemComResultados(sondagem, resultadosParaGravar)
    }

    private fun temAlgumAlunoAvaliado(marcacoes: List<MarcacaoDeNivel>): Boolean {
        for (marcacao in marcacoes) {
            if (marcacao.nivelId != null) {
                return true
            }
        }
        return false
    }

    private fun buscarResultadoDoAluno(resultados: List<ResultadoDaSondagem>, alunoId: String): ResultadoDaSondagem? {
        for (resultado in resultados) {
            if (resultado.alunoId == alunoId) {
                return resultado
            }
        }
        return null
    }

    /**
     * Retorna `null` quando nada mudou para o aluno. "Não avaliado" exclui o
     * resultado anterior; avaliar de novo reaproveita a mesma linha.
     */
    private fun montarResultado(
        sondagemId: String,
        marcacao: MarcacaoDeNivel,
        anterior: ResultadoDaSondagem?,
        agora: Instant,
    ): ResultadoDaSondagem? {
        val nivelId: String? = marcacao.nivelId

        if (nivelId == null) {
            if (anterior == null || anterior.deletedAt != null) {
                return null
            }
            return anterior.copy(deletedAt = agora, updatedAt = agora)
        }

        if (anterior == null) {
            return ResultadoDaSondagem(
                id = UUID.randomUUID().toString(),
                sondagemId = sondagemId,
                alunoId = marcacao.alunoId,
                nivelId = nivelId,
                createdAt = agora,
                updatedAt = agora,
            )
        }

        val mesmoNivel: Boolean = anterior.nivelId == nivelId
        val estavaValendo: Boolean = anterior.deletedAt == null
        if (mesmoNivel && estavaValendo) {
            return null
        }
        return anterior.copy(nivelId = nivelId, deletedAt = null, updatedAt = agora)
    }
}
