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
     * Formatea precio en pesos argentinos (formato contable ej. $ 10.000,00)
     */
    fun formatPrice(value: Double): String {
        val format = java.text.NumberFormat.getCurrencyInstance(localeEs)
        return format.format(value)
    }

    // ─────────────────── Validación de horarios ───────────────────

    /**
     * Convierte una hora en formato "HH:mm" a minutos desde la medianoche.
     * Ej: "09:30" → 570
     */
    fun String.toMinutes(): Int {
        val parts = this.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return h * 60 + m
    }

    /**
     * Determina si dos intervalos horarios se solapan.
     * El solapamiento ocurre cuando: inicio1 < fin2 && fin1 > inicio2
     * Dos clases que van exactamente de 10:00 a 11:00 y de 11:00 a 12:00
     * NO se solapan (el límite es exclusivo).
     */
    fun haySolapamiento(inicio1: String, fin1: String, inicio2: String, fin2: String): Boolean {
        val i1 = inicio1.toMinutes()
        val f1 = fin1.toMinutes()
        val i2 = inicio2.toMinutes()
        val f2 = fin2.toMinutes()
        return i1 < f2 && f1 > i2
    }
}
