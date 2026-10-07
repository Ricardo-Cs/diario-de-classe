package br.com.ricardo.diariodeclasse.data.repository

import br.com.ricardo.diariodeclasse.data.local.dao.MetaDao
import br.com.ricardo.diariodeclasse.data.local.entity.AlunoNaMeta
import br.com.ricardo.diariodeclasse.data.local.entity.Meta
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private class FakeMetaDao : MetaDao {
    val metas = mutableListOf<Meta>()
    val alunos = mutableListOf<AlunoNaMeta>()

    override suspend fun buscarPorId(id: String): Meta? {
        return metas.firstOrNull { meta -> meta.id == id && meta.deletedAt == null }
    }

    override suspend fun buscarTodosOsAlunosDaMeta(metaId: String): List<AlunoNaMeta> {
        return alunos.filter { linha -> linha.metaId == metaId }
    }

    override suspend fun salvarMeta(meta: Meta) {
        metas.removeAll { existente -> existente.id == meta.id }
        metas.add(meta)
    }

    override suspend fun salvarAlunos(alunos: List<AlunoNaMeta>) {
        for (linha in alunos) {
            this.alunos.removeAll { existente -> existente.id == linha.id }
            this.alunos.add(linha)
        }
    }

    override suspend fun marcarComoExcluida(id: String, agora: Instant) {
        val meta: Meta = metas.first { meta -> meta.id == id }
        salvarMeta(meta.copy(deletedAt = agora, updatedAt = agora))
    }

    fun linhaDoAluno(alunoId: String): AlunoNaMeta {
        return alunos.first { linha -> linha.alunoId == alunoId }
    }

    override fun observarDaTurma(turmaId: String): Flow<List<Meta>> = throw UnsupportedOperationException()
    override fun observarPorId(id: String): Flow<Meta?> = throw UnsupportedOperationException()
    override fun observarAlunosDaMeta(metaId: String): Flow<List<AlunoNaMeta>> = throw UnsupportedOperationException()
    override fun observarAlunosDasMetasDaTurma(turmaId: String): Flow<List<AlunoNaMeta>> = throw UnsupportedOperationException()
}

class MetaRepositoryImplTest {
    private val inicio = Instant.parse("2026-10-06T11:00:00Z")
    private val umaHoraDepois = inicio.plusSeconds(3600)
    private val prazo = LocalDate.of(2026, 11, 6)
    private val dao = FakeMetaDao()

    private fun repositorioNoInstante(agora: Instant): MetaRepositoryImpl {
        return MetaRepositoryImpl(dao, Clock.fixed(agora, ZoneOffset.UTC))
    }

    private fun criarMeta(alunos: List<AlunoEscolhidoParaMeta>): Meta = runBlocking {
        repositorioNoInstante(inicio).criar("turma", "Virar alfabéticos", "escrita", "alfabetico", prazo, alunos)
    }

    @Test
    fun criar_gravaAMetaEOsAlunosComONivelInicial() {
        criarMeta(listOf(AlunoEscolhidoParaMeta("ana", "com-valor"), AlunoEscolhidoParaMeta("bruno", null)))

        assertEquals(1, dao.metas.size)
        assertEquals("com-valor", dao.linhaDoAluno("ana").nivelInicialId)
        assertNull(dao.linhaDoAluno("bruno").nivelInicialId)
    }

    @Test
    fun editar_quemContinuaMantemONivelInicialOriginal() = runBlocking {
        val meta: Meta = criarMeta(listOf(AlunoEscolhidoParaMeta("ana", "com-valor")))

        // Na edição a tela manda o nível atual de cada aluno escolhido.
        repositorioNoInstante(umaHoraDepois).editar(
            meta.id, "Nova descrição", "alfabetico", prazo, listOf(AlunoEscolhidoParaMeta("ana", "silabico-alfabetico")),
        )

        assertEquals("com-valor", dao.linhaDoAluno("ana").nivelInicialId)
        assertEquals(inicio, dao.linhaDoAluno("ana").updatedAt)
        assertEquals("Nova descrição", dao.metas.single().descricao)
    }

    @Test
    fun editar_removeQuemSaiuEAcrescentaQuemEntrou() = runBlocking {
        val meta: Meta = criarMeta(listOf(AlunoEscolhidoParaMeta("ana", "com-valor")))

        repositorioNoInstante(umaHoraDepois).editar(
            meta.id, meta.descricao, meta.nivelAlvoId, prazo, listOf(AlunoEscolhidoParaMeta("bruno", "sem-valor")),
        )

        assertEquals(umaHoraDepois, dao.linhaDoAluno("ana").deletedAt)
        assertEquals("sem-valor", dao.linhaDoAluno("bruno").nivelInicialId)
    }

    @Test
    fun alunoQueVolta_reaproveitaALinhaComNivelInicialNovo() = runBlocking {
        val meta: Meta = criarMeta(listOf(AlunoEscolhidoParaMeta("ana", "com-valor")))
        val idOriginal: String = dao.linhaDoAluno("ana").id
        repositorioNoInstante(inicio).editar(meta.id, meta.descricao, meta.nivelAlvoId, prazo, emptyList())

        repositorioNoInstante(umaHoraDepois).editar(
            meta.id, meta.descricao, meta.nivelAlvoId, prazo, listOf(AlunoEscolhidoParaMeta("ana", "silabico-alfabetico")),
        )

        assertEquals(1, dao.alunos.size)
        assertEquals(idOriginal, dao.linhaDoAluno("ana").id)
        assertNull(dao.linhaDoAluno("ana").deletedAt)
        assertEquals("silabico-alfabetico", dao.linhaDoAluno("ana").nivelInicialId)
    }

    @Test
    fun criarMetaAMao_semMetricaESemPrazo() = runBlocking {
        repositorioNoInstante(inicio).criar(
            "turma", "Família numérica do 10 ao 80", null, null, null, listOf(AlunoEscolhidoParaMeta("ana", null)),
        )

        val meta: Meta = dao.metas.single()
        assertNull(meta.metricaId)
        assertNull(meta.prazo)
        assertFalse(meta.acompanhadaPorMetrica())
    }

    @Test
    fun marcarEDesmarcarAtingiu() = runBlocking {
        val meta: Meta = criarMeta(listOf(AlunoEscolhidoParaMeta("ana", null)))
        val hoje: LocalDate = LocalDate.of(2026, 10, 6)

        repositorioNoInstante(umaHoraDepois).marcarAtingiu(meta.id, "ana", hoje)
        assertEquals(hoje, dao.linhaDoAluno("ana").atingiuEm)
        assertEquals(umaHoraDepois, dao.linhaDoAluno("ana").updatedAt)

        repositorioNoInstante(umaHoraDepois).marcarAtingiu(meta.id, "ana", null)
        assertNull(dao.linhaDoAluno("ana").atingiuEm)
    }

    @Test
    fun editar_quemContinuaMantemAMarcacaoDeAtingiu() = runBlocking {
        val meta: Meta = criarMeta(listOf(AlunoEscolhidoParaMeta("ana", null)))
        val hoje: LocalDate = LocalDate.of(2026, 10, 6)
        repositorioNoInstante(inicio).marcarAtingiu(meta.id, "ana", hoje)

        repositorioNoInstante(umaHoraDepois).editar(
            meta.id, "Outra descrição", null, null,
            listOf(AlunoEscolhidoParaMeta("ana", null), AlunoEscolhidoParaMeta("bruno", null)),
        )

        assertEquals(hoje, dao.linhaDoAluno("ana").atingiuEm)
        assertNull(dao.linhaDoAluno("bruno").atingiuEm)
    }

    @Test
    fun encerrarEReabrir() = runBlocking {
        val meta: Meta = criarMeta(emptyList())
        val repositorio = repositorioNoInstante(umaHoraDepois)

        repositorio.encerrar(meta.id, prazo)
        assertEquals(prazo, dao.metas.single().encerradaEm)

        repositorio.reabrir(meta.id)
        assertNull(dao.metas.single().encerradaEm)
    }
}
