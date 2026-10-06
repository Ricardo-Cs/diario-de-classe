package br.com.ricardo.diariodeclasse.data.local

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate

/**
 * O SQLite só conhece tipos primitivos. Os conversores ensinam o Room a gravar
 * [Instant] como epoch em milissegundos (INTEGER) e a ler de volta.
 * [LocalDate] vira texto no formato "2026-10-06": legível no banco e ordenável.
 */
class Converters {
    @TypeConverter
    fun instantParaLong(instant: Instant?): Long? {
        if (instant == null) {
            return null
        }
        return instant.toEpochMilli()
    }

    @TypeConverter
    fun longParaInstant(millis: Long?): Instant? {
        if (millis == null) {
            return null
        }
        return Instant.ofEpochMilli(millis)
    }

    @TypeConverter
    fun localDateParaTexto(data: LocalDate?): String? {
        if (data == null) {
            return null
        }
        return data.toString()
    }

    @TypeConverter
    fun textoParaLocalDate(texto: String?): LocalDate? {
        if (texto == null) {
            return null
        }
        return LocalDate.parse(texto)
    }
}
