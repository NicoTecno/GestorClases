package com.nico.gestorclases.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.nico.gestorclases.data.model.ClaseConAlumnos
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object ExcelExportService {

    fun exportMonthToExcel(context: Context, clasesDelMes: List<ClaseConAlumnos>, mesAnio: String) {
        try {
            val workbook = XSSFWorkbook()

            // Agrupar clases por día
            val clasesPorDia = clasesDelMes.groupBy {
                Instant.ofEpochMilli(it.clase.fecha).atZone(ZoneId.systemDefault()).toLocalDate().dayOfMonth
            }

            // Ordenar los días
            val diasOrdenados = clasesPorDia.keys.sorted()

            for (dia in diasOrdenados) {
                val clasesDia = clasesPorDia[dia]?.sortedBy { it.clase.horaInicio } ?: emptyList()
                val sheet = workbook.createSheet("Día $dia")

                // Cabecera
                val headerRow = sheet.createRow(0)
                headerRow.createCell(0).setCellValue("Hora")
                headerRow.createCell(1).setCellValue("Alumno")
                headerRow.createCell(2).setCellValue("Estado Clase")
                headerRow.createCell(3).setCellValue("Estado Pago")
                headerRow.createCell(4).setCellValue("Precio")
                headerRow.createCell(5).setCellValue("Notas")

                var rowIndex = 1
                for (claseConAlumnos in clasesDia) {
                    val clase = claseConAlumnos.clase
                    val alumnos = claseConAlumnos.alumnos
                    val hora = "${clase.horaInicio} - ${clase.horaFin}"
                    val estadoClase = clase.estadoClase.name

                    if (alumnos.isEmpty()) {
                        val row = sheet.createRow(rowIndex++)
                        row.createCell(0).setCellValue(hora)
                        row.createCell(1).setCellValue("Sin alumnos")
                        row.createCell(2).setCellValue(estadoClase)
                        row.createCell(3).setCellValue("-")
                        row.createCell(4).setCellValue("-")
                        row.createCell(5).setCellValue(clase.notas)
                    } else {
                        for (i in alumnos.indices) {
                            val row = sheet.createRow(rowIndex++)
                            val alumno = alumnos[i]
                            val crossRef = claseConAlumnos.participantes.find { it.alumnoId == alumno.id }
                            
                            // Solo mostrar la hora, estado de clase y notas en la primera fila de la clase
                            if (i == 0) {
                                row.createCell(0).setCellValue(hora)
                                row.createCell(2).setCellValue(estadoClase)
                                row.createCell(5).setCellValue(clase.notas)
                            }

                            row.createCell(1).setCellValue(alumno.nombreCompleto)
                            row.createCell(3).setCellValue(crossRef?.estadoPago?.name ?: "-")
                            row.createCell(4).setCellValue(crossRef?.precioIndividual?.toString() ?: "-")
                        }
                    }
                    
                    // Fila vacía para separar clases
                    rowIndex++
                }
                
                // Set fixed column widths instead of autoSizeColumn (which crashes on Android due to missing java.awt)
                sheet.setColumnWidth(0, 15 * 256) // Hora
                sheet.setColumnWidth(1, 25 * 256) // Alumno
                sheet.setColumnWidth(2, 15 * 256) // Estado Clase
                sheet.setColumnWidth(3, 15 * 256) // Estado Pago
                sheet.setColumnWidth(4, 12 * 256) // Precio
                sheet.setColumnWidth(5, 30 * 256) // Notas
            }

            // Si no hay clases, crear una hoja vacía para que el excel no tire error
            if (workbook.numberOfSheets == 0) {
                val sheet = workbook.createSheet("Sin Clases")
                val row = sheet.createRow(0)
                row.createCell(0).setCellValue("No hay clases registradas para este mes.")
            }

            // Guardar el archivo en caché
            val fileName = "Reporte_Clases_$mesAnio.xlsx"
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { out ->
                workbook.write(out)
            }
            workbook.close()

            // Compartir archivo
            shareExcelFile(context, file)

        } catch (e: Exception) {
            Log.e("ExcelExportService", "Error generando Excel", e)
            android.widget.Toast.makeText(context, "Error al generar Excel: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    private fun shareExcelFile(context: Context, file: File) {
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
