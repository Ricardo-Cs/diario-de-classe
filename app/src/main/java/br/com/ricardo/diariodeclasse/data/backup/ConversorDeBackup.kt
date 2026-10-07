package br.com.ricardo.diariodeclasse.data.backup

import br.com.ricardo.diariodeclasse.data.local.entity.Aluno
import br.com.ricardo.diariodeclasse.data.local.entity.AlunoNaMeta
import br.com.ricardo.diariodeclasse.data.local.entity.Anotacao
import br.com.ricardo.diariodeclasse.data.local.entity.Chamada
import br.com.ricardo.diariodeclasse.data.local.entity.DadosDoDiario
import br.com.ricardo.diariodeclasse.data.local.entity.Meta
import br.com.ricardo.diariodeclasse.data.local.entity.Metrica
import br.com.ricardo.diariodeclasse.data.local.entity.NivelDaMetrica
import br.com.ricardo.diariodeclasse.data.local.entity.Pendencia
import br.com.ricardo.diariodeclasse.data.local.entity.Periodo
import br.com.ricardo.diariodeclasse.data.local.entity.RegistroPresenca
import br.com.ricardo.diariodeclasse.data.local.entity.ResultadoDaSondagem
import br.com.ricardo.diariodeclasse.data.local.entity.Sondagem
import br.com.ricardo.diariodeclasse.data.local.entity.StatusPendencia
import br.com.ricardo.diariodeclasse.data.local.entity.Turma
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeParseException

/** Resultado da leitura de um arquivo escolhido pela professora. */
sealed interface LeituraDoBackup {
    data class Valida(val dados: DadosDoDiario, val exportadoEm: Instant) : LeituraDoBackup
    data class Invalida(val motivo: MotivoDeArquivoInvalido) : LeituraDoBackup
}

enum class MotivoDeArquivoInvalido {
    /** Não é JSON, ou é JSON de outra coisa. */
    NAO_E_BACKUP_DO_APP,

    /** Exportado por uma versão mais nova do app, que este app ainda não sabe ler. */
    VERSAO_MAIS_NOVA,

    /** É deste app, mas está incompleto ou com valores impossíveis (ex.: data inválida). */
    ARQUIVO_DANIFICADO,
}

/**
 * Converte o conteúdo do banco em JSON e de volta. Não depende de Android:
 * é testado com testes unitários comuns.
 *
 * `object` declara uma classe com uma única instância, criada automaticamente
 * (o "singleton" do Kotlin); usamos `ConversorDeBackup.gerarJson(...)` sem `new`.
 */
object ConversorDeBackup {

    /** `prettyPrint` deixa o arquivo indentado, legível se alguém abrir num editor. */
    private val jsonDoArquivo: Json = Json {
        prettyPrint = true
    }

    /** Para o cabeçalho, ignoramos o resto do arquivo (que pode ter campos de versões futuras). */
    private val jsonDoCabecalho: Json = Json {
        ignoreUnknownKeys = true
    }

    fun gerarJson(dados: DadosDoDiario, exportadoEm: Instant): String {
        val arquivo = ArquivoDeBackup(
            formato = FORMATO_DO_ARQUIVO,
            versao = VERSAO_DO_FORMATO,
            exportadoEm = exportadoEm.toString(),
            turmas = dados.turmas.map { turma -> turmaParaArquivo(turma) },
            alunos = dados.alunos.map { aluno -> alunoParaArquivo(aluno) },
            chamadas = dados.chamadas.map { chamada -> chamadaParaArquivo(chamada) },
            registrosPresenca = dados.registrosPresenca.map { registro -> registroParaArquivo(registro) },
            pendencias = dados.pendencias.map { pendencia -> pendenciaParaArquivo(pendencia) },
            anotacoes = dados.anotacoes.map { anotacao -> anotacaoParaArquivo(anotacao) },
            metricas = dados.metricas.map { metrica -> metricaParaArquivo(metrica) },
            niveisDaMetrica = dados.niveisDaMetrica.map { nivel -> nivelParaArquivo(nivel) },
            sondagens = dados.sondagens.map { sondagem -> sondagemParaArquivo(sondagem) },
            resultadosDaSondagem = dados.resultadosDaSondagem.map { resultado -> resultadoParaArquivo(resultado) },
            metas = dados.metas.map { meta -> metaParaArquivo(meta) },
            alunosNaMeta = dados.alunosNaMeta.map { linha -> alunoNaMetaParaArquivo(linha) },
        )
        return jsonDoArquivo.encodeToString(ArquivoDeBackup.serializer(), arquivo)
    }

    /**
     * Lê em duas etapas: primeiro só o cabeçalho (é deste app? de qual versão?),
     * depois o arquivo inteiro. Assim um arquivo de versão mais nova recebe a
     * mensagem certa, em vez de "arquivo danificado".
     */
    fun lerJson(texto: String): LeituraDoBackup {
        val cabecalho: CabecalhoDoArquivo
        try {
            cabecalho = jsonDoCabecalho.decodeFromString(CabecalhoDoArquivo.serializer(), texto)
        } catch (erro: SerializationException) {
            return LeituraDoBackup.Invalida(MotivoDeArquivoInvalido.NAO_E_BACKUP_DO_APP)
        } catch (erro: IllegalArgumentException) {
            return LeituraDoBackup.Invalida(MotivoDeArquivoInvalido.NAO_E_BACKUP_DO_APP)
        }

        if (cabecalho.formato != FORMATO_DO_ARQUIVO) {
            return LeituraDoBackup.Invalida(MotivoDeArquivoInvalido.NAO_E_BACKUP_DO_APP)
        }
        if (cabecalho.versao > VERSAO_DO_FORMATO) {
            return LeituraDoBackup.Invalida(MotivoDeArquivoInvalido.VERSAO_MAIS_NOVA)
        }

        // Datas e enums inválidos só aparecem ao converter para as entidades,
        // por isso a conversão também fica dentro do try.
        try {
            val arquivo: ArquivoDeBackup = jsonDoArquivo.decodeFromString(ArquivoDeBackup.serializer(), texto)
            return LeituraDoBackup.Valida(
                dados = dadosDoArquivo(arquivo),
                exportadoEm = Instant.parse(arquivo.exportadoEm),
            )
        } catch (erro: SerializationException) {
            return LeituraDoBackup.Invalida(MotivoDeArquivoInvalido.ARQUIVO_DANIFICADO)
        } catch (erro: IllegalArgumentException) {
            // Também cobre enum com nome desconhecido (`Periodo.valueOf("XYZ")`).
            return LeituraDoBackup.Invalida(MotivoDeArquivoInvalido.ARQUIVO_DANIFICADO)
        } catch (erro: DateTimeParseException) {
            return LeituraDoBackup.Invalida(MotivoDeArquivoInvalido.ARQUIVO_DANIFICADO)
        }
    }

    private fun dadosDoArquivo(arquivo: ArquivoDeBackup): DadosDoDiario {
        return DadosDoDiario(
            turmas = arquivo.turmas.map { turma -> turmaDoArquivo(turma) },
            alunos = arquivo.alunos.map { aluno -> alunoDoArquivo(aluno) },
            chamadas = arquivo.chamadas.map { chamada -> chamadaDoArquivo(chamada) },
            registrosPresenca = arquivo.registrosPresenca.map { registro -> registroDoArquivo(registro) },
            pendencias = arquivo.pendencias.map { pendencia -> pendenciaDoArquivo(pendencia) },
            anotacoes = arquivo.anotacoes.map { anotacao -> anotacaoDoArquivo(anotacao) },
            metricas = arquivo.metricas.map { metrica -> metricaDoArquivo(metrica) },
            niveisDaMetrica = arquivo.niveisDaMetrica.map { nivel -> nivelDoArquivo(nivel) },
            sondagens = arquivo.sondagens.map { sondagem -> sondagemDoArquivo(sondagem) },
            resultadosDaSondagem = arquivo.resultadosDaSondagem.map { resultado -> resultadoDoArquivo(resultado) },
            metas = arquivo.metas.map { meta -> metaDoArquivo(meta) },
            alunosNaMeta = arquivo.alunosNaMeta.map { linha -> alunoNaMetaDoArquivo(linha) },
        )
    }

    // --- Entidade → arquivo ---

    private fun turmaParaArquivo(turma: Turma): TurmaNoArquivo {
        return TurmaNoArquivo(
            id = turma.id,
            nome = turma.nome,
            anoSerie = turma.anoSerie,
            periodo = turma.periodo.name,
            anoLetivo = turma.anoLetivo,
            createdAt = turma.createdAt.toString(),
            updatedAt = turma.updatedAt.toString(),
            deletedAt = textoOuNulo(turma.deletedAt),
        )
    }

    private fun alunoParaArquivo(aluno: Aluno): AlunoNoArquivo {
        return AlunoNoArquivo(
            id = aluno.id,
            turmaId = aluno.turmaId,
            nome = aluno.nome,
            createdAt = aluno.createdAt.toString(),
            updatedAt = aluno.updatedAt.toString(),
            deletedAt = textoOuNulo(aluno.deletedAt),
        )
    }

    private fun chamadaParaArquivo(chamada: Chamada): ChamadaNoArquivo {
        return ChamadaNoArquivo(
            id = chamada.id,
            turmaId = chamada.turmaId,
            data = chamada.data.toString(),
            createdAt = chamada.createdAt.toString(),
            updatedAt = chamada.updatedAt.toString(),
            deletedAt = textoOuNulo(chamada.deletedAt),
        )
    }

    private fun registroParaArquivo(registro: RegistroPresenca): RegistroPresencaNoArquivo {
        return RegistroPresencaNoArquivo(
            id = registro.id,
            chamadaId = registro.chamadaId,
            alunoId = registro.alunoId,
            presente = registro.presente,
            observacao = registro.observacao,
            createdAt = registro.createdAt.toString(),
            updatedAt = registro.updatedAt.toString(),
            deletedAt = textoOuNulo(registro.deletedAt),
        )
    }

    private fun pendenciaParaArquivo(pendencia: Pendencia): PendenciaNoArquivo {
        return PendenciaNoArquivo(
            id = pendencia.id,
            alunoId = pendencia.alunoId,
            descricao = pendencia.descricao,
            dataLembrete = pendencia.dataLembrete.toString(),
            status = pendencia.status.name,
            registroPresencaId = pendencia.registroPresencaId,
            entregueEm = textoOuNulo(pendencia.entregueEm),
            createdAt = pendencia.createdAt.toString(),
            updatedAt = pendencia.updatedAt.toString(),
            deletedAt = textoOuNulo(pendencia.deletedAt),
        )
    }

    private fun anotacaoParaArquivo(anotacao: Anotacao): AnotacaoNoArquivo {
        return AnotacaoNoArquivo(
            id = anotacao.id,
            alunoId = anotacao.alunoId,
            texto = anotacao.texto,
            data = anotacao.data.toString(),
            createdAt = anotacao.createdAt.toString(),
            updatedAt = anotacao.updatedAt.toString(),
            deletedAt = textoOuNulo(anotacao.deletedAt),
        )
    }

    private fun metricaParaArquivo(metrica: Metrica): MetricaNoArquivo {
        return MetricaNoArquivo(
            id = metrica.id,
            turmaId = metrica.turmaId,
            nome = metrica.nome,
            createdAt = metrica.createdAt.toString(),
            updatedAt = metrica.updatedAt.toString(),
            deletedAt = textoOuNulo(metrica.deletedAt),
        )
    }

    private fun nivelParaArquivo(nivel: NivelDaMetrica): NivelDaMetricaNoArquivo {
        return NivelDaMetricaNoArquivo(
            id = nivel.id,
            metricaId = nivel.metricaId,
            nome = nivel.nome,
            ordem = nivel.ordem,
            createdAt = nivel.createdAt.toString(),
            updatedAt = nivel.updatedAt.toString(),
            deletedAt = textoOuNulo(nivel.deletedAt),
        )
    }

    private fun sondagemParaArquivo(sondagem: Sondagem): SondagemNoArquivo {
        return SondagemNoArquivo(
            id = sondagem.id,
            metricaId = sondagem.metricaId,
            data = sondagem.data.toString(),
            createdAt = sondagem.createdAt.toString(),
            updatedAt = sondagem.updatedAt.toString(),
            deletedAt = textoOuNulo(sondagem.deletedAt),
        )
    }

    private fun resultadoParaArquivo(resultado: ResultadoDaSondagem): ResultadoDaSondagemNoArquivo {
        return ResultadoDaSondagemNoArquivo(
            id = resultado.id,
            sondagemId = resultado.sondagemId,
            alunoId = resultado.alunoId,
            nivelId = resultado.nivelId,
            createdAt = resultado.createdAt.toString(),
            updatedAt = resultado.updatedAt.toString(),
            deletedAt = textoOuNulo(resultado.deletedAt),
        )
    }

    private fun metaParaArquivo(meta: Meta): MetaNoArquivo {
        return MetaNoArquivo(
            id = meta.id,
            turmaId = meta.turmaId,
            descricao = meta.descricao,
            metricaId = meta.metricaId,
            nivelAlvoId = meta.nivelAlvoId,
            prazo = dataOuNulo(meta.prazo),
            encerradaEm = dataOuNulo(meta.encerradaEm),
            createdAt = meta.createdAt.toString(),
            updatedAt = meta.updatedAt.toString(),
            deletedAt = textoOuNulo(meta.deletedAt),
        )
    }

    private fun alunoNaMetaParaArquivo(linha: AlunoNaMeta): AlunoNaMetaNoArquivo {
        return AlunoNaMetaNoArquivo(
            id = linha.id,
            metaId = linha.metaId,
            alunoId = linha.alunoId,
            nivelInicialId = linha.nivelInicialId,
            atingiuEm = dataOuNulo(linha.atingiuEm),
            createdAt = linha.createdAt.toString(),
            updatedAt = linha.updatedAt.toString(),
            deletedAt = textoOuNulo(linha.deletedAt),
        )
    }

    // --- Arquivo → entidade ---

    private fun turmaDoArquivo(turma: TurmaNoArquivo): Turma {
        return Turma(
            id = turma.id,
            nome = turma.nome,
            anoSerie = turma.anoSerie,
            periodo = Periodo.valueOf(turma.periodo),
            anoLetivo = turma.anoLetivo,
            createdAt = Instant.parse(turma.createdAt),
            updatedAt = Instant.parse(turma.updatedAt),
            deletedAt = instanteOuNulo(turma.deletedAt),
        )
    }

    private fun alunoDoArquivo(aluno: AlunoNoArquivo): Aluno {
        return Aluno(
            id = aluno.id,
            turmaId = aluno.turmaId,
            nome = aluno.nome,
            createdAt = Instant.parse(aluno.createdAt),
            updatedAt = Instant.parse(aluno.updatedAt),
            deletedAt = instanteOuNulo(aluno.deletedAt),
        )
    }

    private fun chamadaDoArquivo(chamada: ChamadaNoArquivo): Chamada {
        return Chamada(
            id = chamada.id,
            turmaId = chamada.turmaId,
            data = LocalDate.parse(chamada.data),
            createdAt = Instant.parse(chamada.createdAt),
            updatedAt = Instant.parse(chamada.updatedAt),
            deletedAt = instanteOuNulo(chamada.deletedAt),
        )
    }

    private fun registroDoArquivo(registro: RegistroPresencaNoArquivo): RegistroPresenca {
        return RegistroPresenca(
            id = registro.id,
            chamadaId = registro.chamadaId,
            alunoId = registro.alunoId,
            presente = registro.presente,
            observacao = registro.observacao,
            createdAt = Instant.parse(registro.createdAt),
            updatedAt = Instant.parse(registro.updatedAt),
            deletedAt = instanteOuNulo(registro.deletedAt),
        )
    }

    private fun pendenciaDoArquivo(pendencia: PendenciaNoArquivo): Pendencia {
        return Pendencia(
            id = pendencia.id,
            alunoId = pendencia.alunoId,
            descricao = pendencia.descricao,
            dataLembrete = LocalDate.parse(pendencia.dataLembrete),
            status = StatusPendencia.valueOf(pendencia.status),
            registroPresencaId = pendencia.registroPresencaId,
            entregueEm = instanteOuNulo(pendencia.entregueEm),
            createdAt = Instant.parse(pendencia.createdAt),
            updatedAt = Instant.parse(pendencia.updatedAt),
            deletedAt = instanteOuNulo(pendencia.deletedAt),
        )
    }

    private fun anotacaoDoArquivo(anotacao: AnotacaoNoArquivo): Anotacao {
        return Anotacao(
            id = anotacao.id,
            alunoId = anotacao.alunoId,
            texto = anotacao.texto,
            data = LocalDate.parse(anotacao.data),
            createdAt = Instant.parse(anotacao.createdAt),
            updatedAt = Instant.parse(anotacao.updatedAt),
            deletedAt = instanteOuNulo(anotacao.deletedAt),
        )
    }

    private fun metricaDoArquivo(metrica: MetricaNoArquivo): Metrica {
        return Metrica(
            id = metrica.id,
            turmaId = metrica.turmaId,
            nome = metrica.nome,
            createdAt = Instant.parse(metrica.createdAt),
            updatedAt = Instant.parse(metrica.updatedAt),
            deletedAt = instanteOuNulo(metrica.deletedAt),
        )
    }

    private fun nivelDoArquivo(nivel: NivelDaMetricaNoArquivo): NivelDaMetrica {
        return NivelDaMetrica(
            id = nivel.id,
            metricaId = nivel.metricaId,
            nome = nivel.nome,
            ordem = nivel.ordem,
            createdAt = Instant.parse(nivel.createdAt),
            updatedAt = Instant.parse(nivel.updatedAt),
            deletedAt = instanteOuNulo(nivel.deletedAt),
        )
    }

    private fun sondagemDoArquivo(sondagem: SondagemNoArquivo): Sondagem {
        return Sondagem(
            id = sondagem.id,
            metricaId = sondagem.metricaId,
            data = LocalDate.parse(sondagem.data),
            createdAt = Instant.parse(sondagem.createdAt),
            updatedAt = Instant.parse(sondagem.updatedAt),
            deletedAt = instanteOuNulo(sondagem.deletedAt),
        )
    }

    private fun resultadoDoArquivo(resultado: ResultadoDaSondagemNoArquivo): ResultadoDaSondagem {
        return ResultadoDaSondagem(
            id = resultado.id,
            sondagemId = resultado.sondagemId,
            alunoId = resultado.alunoId,
            nivelId = resultado.nivelId,
            createdAt = Instant.parse(resultado.createdAt),
            updatedAt = Instant.parse(resultado.updatedAt),
            deletedAt = instanteOuNulo(resultado.deletedAt),
        )
    }

    private fun metaDoArquivo(meta: MetaNoArquivo): Meta {
        return Meta(
            id = meta.id,
            turmaId = meta.turmaId,
            descricao = meta.descricao,
            metricaId = meta.metricaId,
            nivelAlvoId = meta.nivelAlvoId,
            prazo = dataLidaOuNulo(meta.prazo),
            encerradaEm = dataLidaOuNulo(meta.encerradaEm),
            createdAt = Instant.parse(meta.createdAt),
            updatedAt = Instant.parse(meta.updatedAt),
            deletedAt = instanteOuNulo(meta.deletedAt),
        )
    }

    private fun alunoNaMetaDoArquivo(linha: AlunoNaMetaNoArquivo): AlunoNaMeta {
        return AlunoNaMeta(
            id = linha.id,
            metaId = linha.metaId,
            alunoId = linha.alunoId,
            nivelInicialId = linha.nivelInicialId,
            atingiuEm = dataLidaOuNulo(linha.atingiuEm),
            createdAt = Instant.parse(linha.createdAt),
            updatedAt = Instant.parse(linha.updatedAt),
            deletedAt = instanteOuNulo(linha.deletedAt),
        )
    }

    private fun dataOuNulo(data: LocalDate?): String? {
        if (data == null) {
            return null
        }
        return data.toString()
    }

    private fun dataLidaOuNulo(texto: String?): LocalDate? {
        if (texto == null) {
            return null
        }
        return LocalDate.parse(texto)
    }

    private fun textoOuNulo(instante: Instant?): String? {
        if (instante == null) {
            return null
        }
        return instante.toString()
    }

    private fun instanteOuNulo(texto: String?): Instant? {
        if (texto == null) {
            return null
        }
        return Instant.parse(texto)
    }
}
