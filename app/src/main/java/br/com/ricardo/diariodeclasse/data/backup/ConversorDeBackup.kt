package br.com.ricardo.diariodeclasse.data.backup

import br.com.ricardo.diariodeclasse.data.local.entity.Aluno
import br.com.ricardo.diariodeclasse.data.local.entity.Anotacao
import br.com.ricardo.diariodeclasse.data.local.entity.Chamada
import br.com.ricardo.diariodeclasse.data.local.entity.DadosDoDiario
import br.com.ricardo.diariodeclasse.data.local.entity.Pendencia
import br.com.ricardo.diariodeclasse.data.local.entity.Periodo
import br.com.ricardo.diariodeclasse.data.local.entity.RegistroPresenca
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
