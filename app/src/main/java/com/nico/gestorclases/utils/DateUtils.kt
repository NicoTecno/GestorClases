package com.nico.gestorclases.utils

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

object DateUtils {

    private val zone = ZoneId.systemDefault()
    private val localeEs = Locale("es", "AR")

    fun LocalDate.toStartOfDayMillis(): Long =
        this.atStartOfDay(zone).toInstant().toEpochMilli()

    fun LocalDate.toEndOfDayMillis(): Long =
        this.atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()

    fun Long.toLocalDate(): LocalDate =
        Instant.ofEpochMilli(this).atZone(zone).toLocalDate()

    fun YearMonth.toStartMillis(): Long = this.atDay(1).toStartOfDayMillis()

    fun YearMonth.toEndMillis(): Long = this.atEndOfMonth().toEndOfDayMillis()

    /**
     * Formatea una fecha como "Lunes, 15 de Septiembre de 2026"
     */
    fun LocalDate.toDisplayString(): String {
        val diaSemana = this.dayOfWeek.getDisplayName(TextStyle.FULL, localeEs)
            .replaceFirstChar { it.uppercase() }
        val mesNombre = this.month.getDisplayName(TextStyle.FULL, localeEs)
            .replaceFirstChar { it.uppercase() }
        return "$diaSemana, ${this.dayOfMonth} de $mesNombre de ${this.year}"
    }

    /**
     * Formatea como "15 de Sep" (short para calendarios)
     */
    fun LocalDate.toShortDisplayString(): String {
        val mesNombre = this.month.getDisplayName(TextStyle.SHORT, localeEs)
            .replaceFirstChar { it.uppercase() }
        return "${this.dayOfMonth} de $mesNombre"
    }

    /**
     * Formatea un YearMonth como "Septiembre 2026"
     */
    fun YearMonth.toDisplayString(): String {
        val mesNombre = this.month.getDisplayName(TextStyle.FULL, localeEs)
            .replaceFirstChar { it.uppercase() }
        return "$mesNombre ${this.year}"
    }

    /**
     * Formatea precio en pesos argentinos
     */
    fun formatPrice(value: Double): String {
        val formattedValue = if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            String.format("%.2f", value)
        }
        return "$ $formattedValue"
    }
}
