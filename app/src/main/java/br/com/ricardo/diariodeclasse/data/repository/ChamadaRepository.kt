package br.com.ricardo.diariodeclasse.data.repository

import br.com.ricardo.diariodeclasse.data.local.dao.ChamadaDao
import br.com.ricardo.diariodeclasse.data.local.entity.Chamada
import br.com.ricardo.diariodeclasse.data.local.entity.RegistroPresenca
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

/** Chamada de um dia junto com a situação de cada aluno. */
data class ChamadaDoDia(
    val chamada: Chamada,
    val registros: List<RegistroPresenca>,
)

/** O que a tela de chamada envia para salvar: a situação de um aluno. */
data class MarcacaoPresenca(
    val alunoId: String,
    val presente: Boolean,
    val observacao: String?,
)

interface ChamadaRepository {
    /** Emite `null` enquanto a chamada daquele dia não foi feita. */
    fun observarChamada(turmaId: String, data: LocalDate): Flow<ChamadaDoDia?>
    suspend fun buscarChamada(turmaId: String, data: LocalDate): ChamadaDoDia?

    /** Cria a chamada do dia ou atualiza a existente. */
    suspend fun salvar(turmaId: String, data: LocalDate, marcacoes: List<MarcacaoPresenca>)
}

class ChamadaRepositoryImpl @Inject constructor(
    private val dao: ChamadaDao,
    private val clock: Clock,
) : ChamadaRepository {

    override fun observarChamada(turmaId: String, data: LocalDate): Flow<ChamadaDoDia?> {
        return combine(
            dao.observarChamada(turmaId, data),
            dao.observarRegistrosDaChamada(turmaId, data),
        ) { chamada, registros ->
            if (chamada == null) {
                null
            } else {
                ChamadaDoDia(chamada, registros)
            }
        }
    }

    override suspend fun buscarChamada(turmaId: String, data: LocalDate): ChamadaDoDia? {
        return observarChamada(turmaId, data).first()
    }

    override suspend fun salvar(turmaId: String, data: LocalDate, marcacoes: List<MarcacaoPresenca>) {
        val agora: Instant = Instant.now(clock)
        val chamadaExistente: ChamadaDoDia? = buscarChamada(turmaId, data)

        val chamada: Chamada
        val registrosAnteriores: List<RegistroPresenca>
        if (chamadaExistente == null) {
            chamada = Chamada(
                id = UUID.randomUUID().toString(),
                turmaId = turmaId,
                data = data,
                createdAt = agora,
                updatedAt = agora,
            )
            registrosAnteriores = emptyList()
        } else {
            chamada = chamadaExistente.chamada.copy(updatedAt = agora)
            registrosAnteriores = chamadaExistente.registros
        }

        val registrosParaGravar = mutableListOf<RegistroPresenca>()
        for (marcacao in marcacoes) {
            val anterior: RegistroPresenca? = buscarRegistroDoAluno(registrosAnteriores, marcacao.alunoId)
            val registro: RegistroPresenca? = montarRegistro(chamada.id, marcacao, anterior, agora)
            if (registro != null) {
                registrosParaGravar.add(registro)
            }
        }

        dao.salvarChamadaComRegistros(chamada, registrosParaGravar)
    }

    private fun buscarRegistroDoAluno(registros: List<RegistroPresenca>, alunoId: String): RegistroPresenca? {
        for (registro in registros) {
            if (registro.alunoId == alunoId) {
                return registro
            }
        }
        return null
    }

    /**
     * Retorna `null` quando nada mudou para o aluno: assim o `updatedAt` só avança
     * nos registros realmente alterados (importante para a futura sincronização).
     */
    private fun montarRegistro(
        chamadaId: String,
        marcacao: MarcacaoPresenca,
        anterior: RegistroPresenca?,
        agora: Instant,
    ): RegistroPresenca? {
        if (anterior == null) {
            return RegistroPresenca(
                id = UUID.randomUUID().toString(),
                chamadaId = chamadaId,
                alunoId = marcacao.alunoId,
                presente = marcacao.presente,
                observacao = marcacao.observacao,
                createdAt = agora,
                updatedAt = agora,
            )
        }

        val mudouPresenca: Boolean = anterior.presente != marcacao.presente
        val mudouObservacao: Boolean = anterior.observacao != marcacao.observacao
        if (!mudouPresenca && !mudouObservacao) {
            return null
        }
        return anterior.copy(
            presente = marcacao.presente,
            observacao = marcacao.observacao,
            updatedAt = agora,
        )
    }
}
