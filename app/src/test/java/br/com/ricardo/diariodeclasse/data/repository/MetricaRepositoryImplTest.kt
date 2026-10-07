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
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** DAO em memória com o que o repositório usa para gravar; as consultas de tela não entram aqui. */
private class FakeMetricaDao : MetricaDao {
    val metricas = mutableListOf<Metrica>()
    val niveis = mutableListOf<NivelDaMetrica>()
    val sondagens = mutableListOf<Sondagem>()
    val resultados = mutableListOf<ResultadoDaSondagem>()

    override suspend fun buscarPorId(id: String): Metrica? {
        return metricas.firstOrNull { metrica -> metrica.id == id && metrica.deletedAt == null }
    }

    override suspend fun buscarTodosOsNiveis(metricaId: String): List<NivelDaMetrica> {
        return niveis.filter { nivel -> nivel.metricaId == metricaId }
    }

    override suspend fun buscarSondagem(metricaId: String, data: LocalDate): Sondagem? {
        return sondagens.firstOrNull { sondagem -> sondagem.metricaId == metricaId && sondagem.data == data }
    }

    override suspend fun buscarResultados(sondagemId: String): List<ResultadoDaSondagem> {
        return resultados.filter { resultado -> resultado.sondagemId == sondagemId }
    }

    override suspend fun salvarMetrica(metrica: Metrica) {
        metricas.removeAll { existente -> existente.id == metrica.id }
        metricas.add(metrica)
    }

    override suspend fun salvarNiveis(niveis: List<NivelDaMetrica>) {
        for (nivel in niveis) {
            this.niveis.removeAll { existente -> existente.id == nivel.id }
            this.niveis.add(nivel)
        }
    }

    override suspend fun salvarSondagem(sondagem: Sondagem) {
        sondagens.removeAll { existente -> existente.id == sondagem.id }
        sondagens.add(sondagem)
    }

    override suspend fun salvarResultados(resultados: List<ResultadoDaSondagem>) {
        for (resultado in resultados) {
            this.resultados.removeAll { existente -> existente.id == resultado.id }
            this.resultados.add(resultado)
        }
    }

    override suspend fun marcarComoExcluida(id: String, agora: Instant) {
        val metrica: Metrica = metricas.first { metrica -> metrica.id == id }
        salvarMetrica(metrica.copy(deletedAt = agora, updatedAt = agora))
    }

    fun nivelChamado(nome: String): NivelDaMetrica {
        return niveis.first { nivel -> nivel.nome == nome }
    }

    fun resultadoDoAluno(alunoId: String): ResultadoDaSondagem {
        return resultados.first { resultado -> resultado.alunoId == alunoId }
    }

    override fun observarDaTurma(turmaId: String): Flow<List<Metrica>> = throw UnsupportedOperationException()
    override fun observarPorId(id: String): Flow<Metrica?> = throw UnsupportedOperationException()
    override fun observarNiveis(metricaId: String): Flow<List<NivelDaMetrica>> = throw UnsupportedOperationException()
    override fun observarNiveisDaTurma(turmaId: String): Flow<List<NivelDaMetrica>> = throw UnsupportedOperationException()
    override suspend fun buscarIdsDeNiveisEmUso(metricaId: String): List<String> = throw UnsupportedOperationException()
    override fun observarResultadosDaTurma(turmaId: String): Flow<List<ResultadoDatado>> = throw UnsupportedOperationException()
    override fun observarResultadosDaMetrica(metricaId: String): Flow<List<ResultadoDatado>> = throw UnsupportedOperationException()
    override fun observarNiveisDoAluno(alunoId: String): Flow<List<NivelRegistradoDoAluno>> = throw UnsupportedOperationException()
    override fun observarSondagens(metricaId: String): Flow<List<SondagemResumida>> = throw UnsupportedOperationException()
}

class MetricaRepositoryImplTest {
    private val inicio = Instant.parse("2026-10-06T11:00:00Z")
    private val umaHoraDepois = inicio.plusSeconds(3600)
    private val hoje = LocalDate.of(2026, 10, 6)
    private val dao = FakeMetricaDao()

    private fun repositorioNoInstante(agora: Instant): MetricaRepositoryImpl {
        return MetricaRepositoryImpl(dao, Clock.fixed(agora, ZoneOffset.UTC))
    }

    private fun niveisNovos(vararg nomes: String): List<NivelEditado> {
        val editados = mutableListOf<NivelEditado>()
        for (nome in nomes) {
            editados.add(NivelEditado(id = null, nome = nome))
        }
        return editados
    }

    private fun criarEscrita(): Metrica = runBlocking {
        repositorioNoInstante(inicio).salvarMetrica(null, "turma", "Escrita", niveisNovos("Silábico", "Alfabético"))
    }

    @Test
    fun criarMetrica_gravaOsNiveisNaOrdemDaLista() {
        criarEscrita()

        assertEquals(1, dao.metricas.size)
        assertEquals(0, dao.nivelChamado("Silábico").ordem)
        assertEquals(1, dao.nivelChamado("Alfabético").ordem)
    }

    @Test
    fun editarMetrica_reordenaRenomeiaERemove() = runBlocking {
        val metrica: Metrica = criarEscrita()
        val silabico: NivelDaMetrica = dao.nivelChamado("Silábico")
        val alfabetico: NivelDaMetrica = dao.nivelChamado("Alfabético")

        val novaLista = listOf(
            NivelEditado(id = alfabetico.id, nome = "Alfabético"),
            NivelEditado(id = null, nome = "Pré-silábico"),
        )
        repositorioNoInstante(umaHoraDepois).salvarMetrica(metrica.id, "turma", "Nível de escrita", novaLista)

        assertEquals("Nível de escrita", dao.metricas.single().nome)
        assertEquals(0, dao.nivelChamado("Alfabético").ordem)
        assertEquals(1, dao.nivelChamado("Pré-silábico").ordem)
        assertEquals(umaHoraDepois, dao.nivelChamado("Silábico").deletedAt)
        assertEquals(silabico.id, dao.nivelChamado("Silábico").id)
    }

    @Test
    fun editarMetrica_naoMexeNoUpdatedAtDeNivelQueNaoMudou() = runBlocking {
        val metrica: Metrica = criarEscrita()
        val silabico: NivelDaMetrica = dao.nivelChamado("Silábico")
        val alfabetico: NivelDaMetrica = dao.nivelChamado("Alfabético")

        val novaLista = listOf(
            NivelEditado(id = silabico.id, nome = "Silábico"),
            NivelEditado(id = alfabetico.id, nome = "Alfabético!"),
        )
        repositorioNoInstante(umaHoraDepois).salvarMetrica(metrica.id, "turma", "Escrita", novaLista)

        assertEquals(inicio, dao.nivelChamado("Silábico").updatedAt)
        assertEquals(umaHoraDepois, dao.nivelChamado("Alfabético!").updatedAt)
    }

    @Test
    fun salvarSondagem_criaUmResultadoPorAlunoAvaliado() = runBlocking {
        val metrica: Metrica = criarEscrita()
        val silabico: String = dao.nivelChamado("Silábico").id
        val marcacoes = listOf(
            MarcacaoDeNivel("ana", silabico),
            MarcacaoDeNivel("bruno", null),
        )

        repositorioNoInstante(inicio).salvarSondagem(metrica.id, hoje, marcacoes)

        val sondagemDoDia: SondagemDoDia? = repositorioNoInstante(inicio).buscarSondagem(metrica.id, hoje)
        assertNotNull(sondagemDoDia)
        assertEquals(listOf("ana"), sondagemDoDia!!.resultados.map { resultado -> resultado.alunoId })
    }

    @Test
    fun sondagemSemNinguemAvaliado_naoECriada() = runBlocking {
        val metrica: Metrica = criarEscrita()

        repositorioNoInstante(inicio).salvarSondagem(metrica.id, hoje, listOf(MarcacaoDeNivel("ana", null)))

        assertEquals(0, dao.sondagens.size)
    }

    @Test
    fun editarSondagem_atualizaSoQuemMudou() = runBlocking {
        val metrica: Metrica = criarEscrita()
        val silabico: String = dao.nivelChamado("Silábico").id
        val alfabetico: String = dao.nivelChamado("Alfabético").id
        repositorioNoInstante(inicio).salvarSondagem(
            metrica.id, hoje, listOf(MarcacaoDeNivel("ana", silabico), MarcacaoDeNivel("bruno", silabico)),
        )

        repositorioNoInstante(umaHoraDepois).salvarSondagem(
            metrica.id, hoje, listOf(MarcacaoDeNivel("ana", alfabetico), MarcacaoDeNivel("bruno", silabico)),
        )

        assertEquals(1, dao.sondagens.size)
        assertEquals(alfabetico, dao.resultadoDoAluno("ana").nivelId)
        assertEquals(umaHoraDepois, dao.resultadoDoAluno("ana").updatedAt)
        assertEquals(inicio, dao.resultadoDoAluno("bruno").updatedAt)
    }

    @Test
    fun naoAvaliadoNaEdicao_excluiOResultadoEAvaliarDeNovoReaproveitaALinha() = runBlocking {
        val metrica: Metrica = criarEscrita()
        val silabico: String = dao.nivelChamado("Silábico").id
        val marcacaoDaAna = MarcacaoDeNivel("ana", silabico)
        val marcacaoDoBruno = MarcacaoDeNivel("bruno", silabico)
        repositorioNoInstante(inicio).salvarSondagem(metrica.id, hoje, listOf(marcacaoDaAna, marcacaoDoBruno))
        val idOriginal: String = dao.resultadoDoAluno("ana").id

        repositorioNoInstante(umaHoraDepois).salvarSondagem(
            metrica.id, hoje, listOf(MarcacaoDeNivel("ana", null), marcacaoDoBruno),
        )
        assertEquals(umaHoraDepois, dao.resultadoDoAluno("ana").deletedAt)

        repositorioNoInstante(umaHoraDepois).salvarSondagem(metrica.id, hoje, listOf(marcacaoDaAna, marcacaoDoBruno))
        assertNull(dao.resultadoDoAluno("ana").deletedAt)
        assertEquals(idOriginal, dao.resultadoDoAluno("ana").id)
    }

    @Test
    fun editarSondagemDeixandoTodosSemAvaliacao_excluiASondagem() = runBlocking {
        val metrica: Metrica = criarEscrita()
        val silabico: String = dao.nivelChamado("Silábico").id
        repositorioNoInstante(inicio).salvarSondagem(metrica.id, hoje, listOf(MarcacaoDeNivel("ana", silabico)))

        repositorioNoInstante(umaHoraDepois).salvarSondagem(metrica.id, hoje, listOf(MarcacaoDeNivel("ana", null)))

        assertNull(repositorioNoInstante(umaHoraDepois).buscarSondagem(metrica.id, hoje))
        assertEquals(umaHoraDepois, dao.sondagens.single().deletedAt)
    }

    @Test
    fun sondagemExcluida_eReaproveitadaAoAvaliarDeNovoNoMesmoDia() = runBlocking {
        val metrica: Metrica = criarEscrita()
        val silabico: String = dao.nivelChamado("Silábico").id
        repositorioNoInstante(inicio).salvarSondagem(metrica.id, hoje, listOf(MarcacaoDeNivel("ana", silabico)))
        repositorioNoInstante(inicio).salvarSondagem(metrica.id, hoje, listOf(MarcacaoDeNivel("ana", null)))

        repositorioNoInstante(umaHoraDepois).salvarSondagem(metrica.id, hoje, listOf(MarcacaoDeNivel("ana", silabico)))

        assertEquals(1, dao.sondagens.size)
        assertNull(dao.sondagens.single().deletedAt)
        assertNotNull(repositorioNoInstante(umaHoraDepois).buscarSondagem(metrica.id, hoje))
    }
}
