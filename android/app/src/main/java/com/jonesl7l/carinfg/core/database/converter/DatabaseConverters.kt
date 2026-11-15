package com.jonesl7l.carinfg.core.database.converter

import androidx.room3.ColumnTypeConverter
import com.jonesl7l.carinfg.core.database.entity.incident.PersonColumn
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json

class DatabaseConverters {
    @ColumnTypeConverter
    fun instantToEpochMillis(value: Instant?): Long? = value?.toEpochMilliseconds()

    @ColumnTypeConverter
    fun epochMillisToInstant(value: Long?): Instant? = value?.let(Instant::fromEpochMilliseconds)

    @ColumnTypeConverter
    fun localDateToIso(value: LocalDate?): String? = value?.toString()

    @ColumnTypeConverter
    fun isoToLocalDate(value: String?): LocalDate? = value?.let { LocalDate.parse(input = it) }

    @ColumnTypeConverter
    fun stringsToJson(value: List<String>): String = json.encodeToString(value)

    @ColumnTypeConverter
    fun jsonToStrings(value: String): List<String> = json.decodeFromString(value)

    @ColumnTypeConverter
    fun personsToJson(value: List<PersonColumn>): String = json.encodeToString(value)

    @ColumnTypeConverter
    fun jsonToPersons(value: String): List<PersonColumn> = json.decodeFromString(value)

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
    }
}
