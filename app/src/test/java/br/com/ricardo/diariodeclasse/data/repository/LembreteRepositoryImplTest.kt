package br.com.ricardo.diariodeclasse.data.repository

import br.com.ricardo.diariodeclasse.data.local.dao.LembreteDao
import br.com.ricardo.diariodeclasse.data.local.entity.Lembrete
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** DAO em memória; `buscarAte` imita o filtro da consulta real. */
private class FakeLembreteDao : LembreteDao {
    val lembretes = mutableListOf<Lembrete>()

    override suspend fun buscarAte(data: LocalDate): List<Lembrete> {
        return lembretes.filter { lembrete ->
            lembrete.concluidoEm == null && lembrete.deletedAt == null && !lembrete.data.isAfter(data)
        }
    }

    override suspend fun buscarPorId(id: String): Lembrete? {
        return lembretes.firstOrNull { lembrete -> lembrete.id == id }
    }

    override suspend fun inserir(lembrete: Lembrete) {
        lembretes.add(lembrete)
    }

    override suspend fun atualizar(lembrete: Lembrete) {
        lembretes.removeAll { existente -> existente.id == lembrete.id }
        lembretes.add(lembrete)
    }

    override suspend fun marcarComoExcluido(id: String, agora: Instant) {
        val lembrete: Lembrete = lembretes.first { lembrete -> lembrete.id == id }
        atualizar(lembrete.copy(deletedAt = agora, updatedAt = agora))
    }

    override fun observarEmAberto(): Flow<List<Lembrete>> = throw UnsupportedOperationException()
    override fun observarConcluidos(limite: Int): Flow<List<Lembrete>> = throw UnsupportedOperationException()
}

class LembreteRepositoryImplTest {
    private val inicio = Instant.parse("2026-10-06T11:00:00Z")
    private val umaHoraDepois = inicio.plusSeconds(3600)
    private val hoje = LocalDate.of(2026, 10, 6)
    private val dao = FakeLembreteDao()

    private fun repositorioNoInstante(agora: Instant): LembreteRepositoryImpl {
        return LembreteRepositoryImpl(dao, Clock.fixed(agora, ZoneOffset.UTC))
    }

    @Test
    fun concluirEReabrir() = runBlocking {
        val lembrete: Lembrete = repositorioNoInstante(inicio).criar("Entregar portfólio", hoje)

        repositorioNoInstante(umaHoraDepois).marcarComoConcluido(lembrete.id)
        assertEquals(umaHoraDepois, dao.lembretes.single().concluidoEm)

        repositorioNoInstante(umaHoraDepois).reabrir(lembrete.id)
        assertNull(dao.lembretes.single().concluidoEm)
    }

    @Test
    fun paraNotificar_trazOsDeHojeEOsAtrasadosEmAberto() = runBlocking {
        val repositorio = repositorioNoInstante(inicio)
        repositorio.criar("Atrasado", hoje.minusDays(2))
        repositorio.criar("Hoje", hoje)
        repositorio.criar("Amanhã", hoje.plusDays(1))
        val concluido: Lembrete = repositorio.criar("Já feito", hoje)
        repositorio.marcarComoConcluido(concluido.id)

        val paraNotificar: List<Lembrete> = repositorio.buscarParaNotificar(hoje)

        assertEquals(setOf("Atrasado", "Hoje"), paraNotificar.map { lembrete -> lembrete.descricao }.toSet())
    }

    @Test
    fun excluirERestaurar() = runBlocking {
        val lembrete: Lembrete = repositorioNoInstante(inicio).criar("Plano de ação", hoje)

        repositorioNoInstante(umaHoraDepois).excluir(lembrete.id)
        assertEquals(umaHoraDepois, dao.lembretes.single().deletedAt)

        repositorioNoInstante(umaHoraDepois).restaurar(lembrete.id)
        assertNull(dao.lembretes.single().deletedAt)
    }

    @Test
    fun editar_trocaDescricaoEData() = runBlocking {
        val lembrete: Lembrete = repositorioNoInstante(inicio).criar("Portfólio", hoje)

        repositorioNoInstante(umaHoraDepois).editar(lembrete.id, "Portfólio socioemocional", hoje.plusDays(10))

        val editado: Lembrete = dao.lembretes.single()
        assertEquals("Portfólio socioemocional", editado.descricao)
        assertEquals(hoje.plusDays(10), editado.data)
        assertEquals(umaHoraDepois, editado.updatedAt)
    }
}
