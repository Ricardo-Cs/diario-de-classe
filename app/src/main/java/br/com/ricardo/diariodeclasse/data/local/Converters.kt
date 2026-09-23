package br.com.ricardo.diariodeclasse.data.local

import androidx.room.TypeConverter
import java.time.Instant

/**
 * O SQLite só conhece tipos primitivos. Os conversores ensinam o Room a gravar
 * [Instant] como epoch em milissegundos (INTEGER) e a ler de volta.
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
}
