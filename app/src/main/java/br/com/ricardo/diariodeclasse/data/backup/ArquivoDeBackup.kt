package br.com.ricardo.diariodeclasse.data.backup

import kotlinx.serialization.Serializable

/*
 * Formato do JSON da exportação (vai dentro do .zip, ver PacoteDeBackup). São classes separadas das entidades do
 * Room de propósito: o banco pode mudar (colunas novas, nomes diferentes) e o
 * arquivo exportado hoje precisa continuar sendo lido pelas versões futuras do app.
 * Quando o formato mudar, incremente [VERSAO_DO_FORMATO] e trate a leitura das
 * versões antigas em ConversorDeBackup.
 *
 * `@Serializable` faz o plugin do kotlinx.serialization gerar, na compilação, o
 * código que converte a classe em JSON e de volta (como o Jackson, sem reflexão).
 *
 * Datas e instantes vão como texto ISO ("2026-10-06", "2026-10-06T14:30:00Z") e
 * enums pelo nome ("MANHA"), para o arquivo ser legível por uma pessoa.
 */

/** Identifica um arquivo deste app; impede importar um JSON qualquer por engano. */
const val FORMATO_DO_ARQUIVO = "diario-de-classe"

/**
 * Histórico:
 * 1 → turmas, alunos, chamadas, pendências e anotações
 * 2 → métricas (níveis, sondagens e resultados) e metas. Os campos novos têm
 *     lista vazia como padrão, então arquivos da versão 1 continuam sendo lidos.
 * 3 → metas livres: métrica, nível-alvo e prazo opcionais; aluno na meta com "atingiu em".
 *     Um app da versão 2 não leria uma meta sem métrica, por isso o número mudou.
 * 4 → lembretes da professora (lista vazia como padrão para arquivos antigos).
 * 5 → fotos do registro do dia. O JSON passa a ir dentro de um .zip, junto com as
 *     imagens; um .json avulso (versões 1 a 4) continua sendo importado.
 */
const val VERSAO_DO_FORMATO = 5

/** Só o começo do arquivo: lido antes do resto para checar formato e versão. */
@Serializable
data class CabecalhoDoArquivo(
    val formato: String,
    val versao: Int,
)

@Serializable
data class ArquivoDeBackup(
    val formato: String,
    val versao: Int,
    val exportadoEm: String,
    val turmas: List<TurmaNoArquivo>,
    val alunos: List<AlunoNoArquivo>,
    val chamadas: List<ChamadaNoArquivo>,
    val registrosPresenca: List<RegistroPresencaNoArquivo>,
    val pendencias: List<PendenciaNoArquivo>,
    val anotacoes: List<AnotacaoNoArquivo>,
    val metricas: List<MetricaNoArquivo> = emptyList(),
    val niveisDaMetrica: List<NivelDaMetricaNoArquivo> = emptyList(),
    val sondagens: List<SondagemNoArquivo> = emptyList(),
    val resultadosDaSondagem: List<ResultadoDaSondagemNoArquivo> = emptyList(),
    val metas: List<MetaNoArquivo> = emptyList(),
    val alunosNaMeta: List<AlunoNaMetaNoArquivo> = emptyList(),
    val lembretes: List<LembreteNoArquivo> = emptyList(),
    val fotos: List<FotoNoArquivo> = emptyList(),
)

@Serializable
data class TurmaNoArquivo(
    val id: String,
    val nome: String,
    val anoSerie: String,
    val periodo: String,
    val anoLetivo: Int,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String? = null,
)

@Serializable
data class AlunoNoArquivo(
    val id: String,
    val turmaId: String,
    val nome: String,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String? = null,
)

@Serializable
data class ChamadaNoArquivo(
    val id: String,
    val turmaId: String,
    val data: String,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String? = null,
)

@Serializable
data class RegistroPresencaNoArquivo(
    val id: String,
    val chamadaId: String,
    val alunoId: String,
    val presente: Boolean,
    val observacao: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String? = null,
)

@Serializable
data class PendenciaNoArquivo(
    val id: String,
    val alunoId: String,
    val descricao: String,
    val dataLembrete: String,
    val status: String,
    val registroPresencaId: String? = null,
    val entregueEm: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String? = null,
)

@Serializable
data class AnotacaoNoArquivo(
    val id: String,
    val alunoId: String,
    val texto: String,
    val data: String,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String? = null,
)

@Serializable
data class MetricaNoArquivo(
    val id: String,
    val turmaId: String,
    val nome: String,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String? = null,
)

@Serializable
data class NivelDaMetricaNoArquivo(
    val id: String,
    val metricaId: String,
    val nome: String,
    val ordem: Int,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String? = null,
)

@Serializable
data class SondagemNoArquivo(
    val id: String,
    val metricaId: String,
    val data: String,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String? = null,
)

@Serializable
data class ResultadoDaSondagemNoArquivo(
    val id: String,
    val sondagemId: String,
    val alunoId: String,
    val nivelId: String,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String? = null,
)

@Serializable
data class MetaNoArquivo(
    val id: String,
    val turmaId: String,
    val descricao: String,
    val metricaId: String? = null,
    val nivelAlvoId: String? = null,
    val prazo: String? = null,
    val encerradaEm: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String? = null,
)

@Serializable
data class AlunoNaMetaNoArquivo(
    val id: String,
    val metaId: String,
    val alunoId: String,
    val nivelInicialId: String? = null,
    val atingiuEm: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String? = null,
)

@Serializable
data class LembreteNoArquivo(
    val id: String,
    val descricao: String,
    val data: String,
    val concluidoEm: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String? = null,
)

/** Os dados da foto; a imagem vai no .zip como "fotos/[nomeDoArquivo]". */
@Serializable
data class FotoNoArquivo(
    val id: String,
    val turmaId: String,
    val data: String,
    val nomeDoArquivo: String,
    val legenda: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String? = null,
)
