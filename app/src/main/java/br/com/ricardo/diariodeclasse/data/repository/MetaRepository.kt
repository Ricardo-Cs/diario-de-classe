package br.com.ricardo.diariodeclasse.data.repository

import br.com.ricardo.diariodeclasse.data.local.dao.MetaDao
import br.com.ricardo.diariodeclasse.data.local.entity.AlunoNaMeta
import br.com.ricardo.diariodeclasse.data.local.entity.Meta
import kotlinx.coroutines.flow.Flow
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

/** Um aluno escolhido no formulário da meta, com o nível em que estava naquele momento. */
data class AlunoEscolhidoParaMeta(
    val alunoId: String,
    val nivelInicialId: String?,
)

interface MetaRepository {
    /** Em andamento primeiro, pelo prazo mais próximo; as encerradas por último. */
    fun observarMetasDaTurma(turmaId: String): Flow<List<Meta>>
    fun observarMeta(id: String): Flow<Meta?>
    suspend fun buscarMeta(id: String): Meta?

    fun observarAlunosDaMeta(metaId: String): Flow<List<AlunoNaMeta>>
    fun observarAlunosDasMetasDaTurma(turmaId: String): Flow<List<AlunoNaMeta>>

    /** `metricaId` e `nivelAlvoId` `null` = meta acompanhada marcando à mão. */
    suspend fun criar(
        turmaId: String,
        descricao: String,
        metricaId: String?,
        nivelAlvoId: String?,
        prazo: LocalDate?,
        alunos: List<AlunoEscolhidoParaMeta>,
    ): Meta

    /**
     * A forma de acompanhar e a métrica não mudam na edição: trocar seria outra meta.
     * Quem continua na meta mantém o nível inicial original e a marcação de "atingiu".
     */
    suspend fun editar(
        metaId: String,
        descricao: String,
        nivelAlvoId: String?,
        prazo: LocalDate?,
        alunos: List<AlunoEscolhidoParaMeta>,
    )

    /** Metas acompanhadas à mão: [atingiuEm] `null` desmarca o aluno. */
    suspend fun marcarAtingiu(metaId: String, alunoId: String, atingiuEm: LocalDate?)

    suspend fun encerrar(metaId: String, data: LocalDate)
    suspend fun reabrir(metaId: String)
    suspend fun excluir(metaId: String)
}

class MetaRepositoryImpl @Inject constructor(
    private val dao: MetaDao,
    private val clock: Clock,
) : MetaRepository {

    override fun observarMetasDaTurma(turmaId: String): Flow<List<Meta>> {
        return dao.observarDaTurma(turmaId)
    }

    override fun observarMeta(id: String): Flow<Meta?> {
        return dao.observarPorId(id)
    }

    override suspend fun buscarMeta(id: String): Meta? {
        return dao.buscarPorId(id)
    }

    override fun observarAlunosDaMeta(metaId: String): Flow<List<AlunoNaMeta>> {
        return dao.observarAlunosDaMeta(metaId)
    }

    override fun observarAlunosDasMetasDaTurma(turmaId: String): Flow<List<AlunoNaMeta>> {
        return dao.observarAlunosDasMetasDaTurma(turmaId)
    }

    override suspend fun criar(
        turmaId: String,
        descricao: String,
        metricaId: String?,
        nivelAlvoId: String?,
        prazo: LocalDate?,
        alunos: List<AlunoEscolhidoParaMeta>,
    ): Meta {
        val agora: Instant = Instant.now(clock)
        val meta = Meta(
            id = UUID.randomUUID().toString(),
            turmaId = turmaId,
            descricao = descricao,
            metricaId = metricaId,
            nivelAlvoId = nivelAlvoId,
            prazo = prazo,
            createdAt = agora,
            updatedAt = agora,
        )

        val linhas = mutableListOf<AlunoNaMeta>()
        for (escolhido in alunos) {
            linhas.add(novaLinha(meta.id, escolhido, agora))
        }

        dao.salvarMetaComAlunos(meta, linhas)
        return meta
    }

    override suspend fun editar(
        metaId: String,
        descricao: String,
        nivelAlvoId: String?,
        prazo: LocalDate?,
        alunos: List<AlunoEscolhidoParaMeta>,
    ) {
        val metaExistente: Meta = dao.buscarPorId(metaId) ?: return
        val agora: Instant = Instant.now(clock)
        val meta: Meta = metaExistente.copy(
            descricao = descricao,
            nivelAlvoId = nivelAlvoId,
            prazo = prazo,
            updatedAt = agora,
        )

        val linhasAntigas: List<AlunoNaMeta> = dao.buscarTodosOsAlunosDaMeta(metaId)
        val linhasParaGravar = mutableListOf<AlunoNaMeta>()

        for (escolhido in alunos) {
            val antiga: AlunoNaMeta? = buscarLinhaDoAluno(linhasAntigas, escolhido.alunoId)
            if (antiga == null) {
                linhasParaGravar.add(novaLinha(metaId, escolhido, agora))
            } else if (antiga.deletedAt != null) {
                // O aluno tinha saído da meta e voltou: o nível inicial passa a ser o de agora.
                val devolvida = antiga.copy(nivelInicialId = escolhido.nivelInicialId, deletedAt = null, updatedAt = agora)
                linhasParaGravar.add(devolvida)
            }
        }

        val idsEscolhidos = mutableSetOf<String>()
        for (escolhido in alunos) {
            idsEscolhidos.add(escolhido.alunoId)
        }
        for (antiga in linhasAntigas) {
            val estavaNaMeta: Boolean = antiga.deletedAt == null
            if (estavaNaMeta && antiga.alunoId !in idsEscolhidos) {
                linhasParaGravar.add(antiga.copy(deletedAt = agora, updatedAt = agora))
            }
        }

        dao.salvarMetaComAlunos(meta, linhasParaGravar)
    }

    private fun novaLinha(metaId: String, escolhido: AlunoEscolhidoParaMeta, agora: Instant): AlunoNaMeta {
        return AlunoNaMeta(
            id = UUID.randomUUID().toString(),
            metaId = metaId,
            alunoId = escolhido.alunoId,
            nivelInicialId = escolhido.nivelInicialId,
            createdAt = agora,
            updatedAt = agora,
        )
    }

    private fun buscarLinhaDoAluno(linhas: List<AlunoNaMeta>, alunoId: String): AlunoNaMeta? {
        for (linha in linhas) {
            if (linha.alunoId == alunoId) {
                return linha
            }
        }
        return null
    }

    override suspend fun marcarAtingiu(metaId: String, alunoId: String, atingiuEm: LocalDate?) {
        val linhas: List<AlunoNaMeta> = dao.buscarTodosOsAlunosDaMeta(metaId)
        val linha: AlunoNaMeta = buscarLinhaDoAluno(linhas, alunoId) ?: return
        if (linha.atingiuEm == atingiuEm) {
            return
        }
        val marcada = linha.copy(atingiuEm = atingiuEm, updatedAt = Instant.now(clock))
        dao.salvarAlunos(listOf(marcada))
    }

    override suspend fun encerrar(metaId: String, data: LocalDate) {
        val meta: Meta = dao.buscarPorId(metaId) ?: return
        dao.salvarMeta(meta.copy(encerradaEm = data, updatedAt = Instant.now(clock)))
    }

    override suspend fun reabrir(metaId: String) {
        val meta: Meta = dao.buscarPorId(metaId) ?: return
        dao.salvarMeta(meta.copy(encerradaEm = null, updatedAt = Instant.now(clock)))
    }

    override suspend fun excluir(metaId: String) {
        dao.marcarComoExcluida(metaId, Instant.now(clock))
    }
}
