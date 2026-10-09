package br.com.ricardo.diariodeclasse.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Perfil da professora: nome, escola e foto, mostrados na aba Perfil e na
 * saudação do Início. Existe no máximo um por aparelho; é criado na primeira
 * vez que ela salva o nome.
 *
 * É uma tabela, e não uma preferência, para seguir as regras das outras
 * entidades (UUID, datas, exclusão suave) e ir junto na exportação.
 */
@Entity(tableName = "perfil")
data class Perfil(
    @PrimaryKey override val id: String,
    val nome: String,
    /** Opcional, ex.: "EMEF Monteiro Lobato". */
    val escola: String? = null,
    /** Arquivo da foto na mesma pasta das fotos do dia (ver `Foto`); `null` = sem foto. */
    val nomeDoArquivoDaFoto: String? = null,
    override val createdAt: Instant,
    override val updatedAt: Instant,
    override val deletedAt: Instant? = null,
) : EntidadeBase
