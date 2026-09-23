package br.com.ricardo.diariodeclasse.data.local.entity

import java.time.Instant

/**
 * Contrato comum a todas as entidades, preparando a futura sincronização com backend:
 * - [id] em UUID (gerado no aparelho, sem colisão entre dispositivos);
 * - [createdAt]/[updatedAt] para resolver conflitos de sincronização;
 * - [deletedAt] para soft delete: a linha continua no banco e as consultas padrão a ignoram.
 */
interface EntidadeBase {
    val id: String
    val createdAt: Instant
    val updatedAt: Instant
    val deletedAt: Instant?
}
