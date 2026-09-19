package com.nico.gestorclases.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.nico.gestorclases.data.model.ClaseConAlumnos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.ZoneId

/**
 * Servicio utilitario para exportar el reporte mensual de clases a un archivo Excel (.xlsx).
 *
 * La API está dividida en dos funciones con responsabilidades distintas:
 * - [generarArchivoExcel]: suspend function que hace el I/O pesado en [Dispatchers.IO].
 * - [compartirArchivo]: función normal que debe llamarse desde el hilo principal (Main).
 *
 * Uso desde un Composable:
 * ```kotlin
 * val scope = rememberCoroutineScope()
 * scope.launch {
 *     val file = ExcelExportService.generarArchivoExcel(context, clases, mes)
 *     ExcelExportService.compartirArchivo(context, file)
 * }
 * ```
 */
object ExcelExportService {

    /**
     * Genera el archivo Excel con el reporte mensual de clases.
     *
     * Corre completamente en [Dispatchers.IO] (operación de I/O pesada con Apache POI).
     * El llamador recupera el [File] resultante en el hilo desde el que llamó (Main si
     * fue lanzado con [rememberCoroutineScope]).
     *
     * @param context Contexto de la aplicación para acceder a [Context.getCacheDir].
     * @param clasesDelMes Lista de clases del mes a exportar.
     * @param mesAnio String identificador del mes (ej: "2026-09") usado en el nombre del archivo.
     * @return El [File] generado en el directorio de caché.
     */
    suspend fun generarArchivoExcel(
        context: Context,
        clasesDelMes: List<ClaseConAlumnos>,
        mesAnio: String
    ): File = withContext(Dispatchers.IO) {
        val workbook = XSSFWorkbook()

        // Agrupar y ordenar clases por día del mes
        val clasesPorDia = clasesDelMes.groupBy {
            Instant.ofEpochMilli(it.clase.fecha)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .dayOfMonth
        }

        for (dia in clasesPorDia.keys.sorted()) {
            val clasesDia = clasesPorDia[dia]?.sortedBy { it.clase.horaInicio } ?: emptyList()
            val sheet = workbook.createSheet("Día $dia")

            // ── Fila de cabecera ──────────────────────────────────────────────
            val headerRow = sheet.createRow(0)
            headerRow.createCell(0).setCellValue("Hora")
            headerRow.createCell(1).setCellValue("Alumno")
            headerRow.createCell(2).setCellValue("Estado Clase")
            headerRow.createCell(3).setCellValue("Estado Pago")
            headerRow.createCell(4).setCellValue("Precio")
            headerRow.createCell(5).setCellValue("Notas")

            // ── Filas de datos ────────────────────────────────────────────────
            var rowIndex = 1
            for (claseConAlumnos in clasesDia) {
                val clase = claseConAlumnos.clase
                val alumnos = claseConAlumnos.alumnos
                val hora = "${clase.horaInicio} - ${clase.horaFin}"
                val estadoClaseLabel = clase.estadoClase.displayName

                if (alumnos.isEmpty()) {
                    val row = sheet.createRow(rowIndex++)
                    row.createCell(0).setCellValue(hora)
                    row.createCell(1).setCellValue("Sin alumnos")
                    row.createCell(2).setCellValue(estadoClaseLabel)
                    row.createCell(3).setCellValue("-")
                    row.createCell(4).setCellValue("-")
                    row.createCell(5).setCellValue(clase.notas)
                } else {
                    for (i in alumnos.indices) {
                        val row = sheet.createRow(rowIndex++)
                        val alumno = alumnos[i]
                        val crossRef = claseConAlumnos.participantes.find { it.alumnoId == alumno.id }

                        // Solo la primera fila de la clase lleva hora, estado y notas
                        if (i == 0) {
                            row.createCell(0).setCellValue(hora)
                            row.createCell(2).setCellValue(estadoClaseLabel)
                            row.createCell(5).setCellValue(clase.notas)
                        }

                        row.createCell(1).setCellValue(alumno.nombreCompleto)
                        row.createCell(3).setCellValue(crossRef?.estadoPago?.displayName ?: "-")
                        row.createCell(4).setCellValue(crossRef?.precioIndividual?.toString() ?: "-")
                    }
                }

                // Fila vacía para separar clases visualmente
                rowIndex++
            }

            // Anchos fijos de columna (autoSizeColumn usa java.awt, no disponible en Android)
            sheet.setColumnWidth(0, 15 * 256) // Hora
            sheet.setColumnWidth(1, 25 * 256) // Alumno
            sheet.setColumnWidth(2, 15 * 256) // Estado Clase
            sheet.setColumnWidth(3, 15 * 256) // Estado Pago
            sheet.setColumnWidth(4, 12 * 256) // Precio
            sheet.setColumnWidth(5, 30 * 256) // Notas
        }

        // Hoja de fallback si no hay clases en el mes
        if (workbook.numberOfSheets == 0) {
            val sheet = workbook.createSheet("Sin Clases")
            sheet.createRow(0).createCell(0)
                .setCellValue("No hay clases registradas para este mes.")
        }

        // ── Escribir el archivo en caché ──────────────────────────────────────
        val fileName = "Reporte_Clases_$mesAnio.xlsx"
        val file = File(context.cacheDir, fileName)
        FileOutputStream(file).use { out -> workbook.write(out) }
        workbook.close()

        Log.d("ExcelExportService", "Archivo generado: ${file.absolutePath}")
        file
    }

    /**
     * Abre el selector de apps para compartir el archivo Excel.
     *
     * IMPORTANTE: debe llamarse desde el hilo principal (Main dispatcher).
     * Cuando se llama después de [generarArchivoExcel] en una corrutina lanzada con
     * [rememberCoroutineScope], el contexto ya habrá vuelto a Main automáticamente.
     *
     * @param context Contexto necesario para [FileProvider] y [Context.startActivity].
     * @param file El archivo generado por [generarArchivoExcel].
     */
    fun compartirArchivo(context: Context, file: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Compartir Reporte Mensual"))
    }
}
