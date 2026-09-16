package com.nico.gestorclases.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.nico.gestorclases.data.model.EstadoPago
import com.nico.gestorclases.utils.DateUtils.formatPrice
import com.nico.gestorclases.utils.DateUtils.toLocalDate
import com.nico.gestorclases.viewmodel.CierreDelMesInfo

object PdfExporter {

    fun exportCierreToPdf(context: Context, uri: Uri, info: CierreDelMesInfo): Boolean {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subtitlePaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        var y = 50f
        val left = 50f
        val pageWidth = 545f

        // ── Título ──────────────────────────────────────────────────────────
        canvas.drawText("Reporte de Cierre de Mes", left, y, titlePaint)
        y += 30f
        canvas.drawText("Mes: ${info.mesAnio}", left, y, paint)
        y += 40f

        // ── Resumen ─────────────────────────────────────────────────────────
        canvas.drawText("Clases Dadas: ${info.clasesDadas}", left, y, subtitlePaint)
        y += 22f
        canvas.drawText("Total Facturable: ${formatPrice(info.montoTotalFacturable)}", left, y, paint)
        y += 18f
        canvas.drawText("Cobrado: ${formatPrice(info.montoCobrado)}", left, y, paint)
        y += 18f
        canvas.drawText("Pendiente: ${formatPrice(info.montoPendiente)}", left, y, paint)
        y += 35f

        // ── Detalle por alumno ───────────────────────────────────────────────
        if (info.alumnosConDeuda.isNotEmpty()) {
            canvas.drawText("Deudores del mes", left, y, subtitlePaint)
            y += 25f

            for (deudor in info.alumnosConDeuda) {
                if (y > 790f) break  // evitar desborde de página

                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("• ${deudor.alumno.nombreCompleto}", left, y, paint)
                y += 18f

                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                for (claseConAlumnos in deudor.clases) {
                    if (y > 790f) break
                    val ref = claseConAlumnos.participantes.find {
                        it.alumnoId == deudor.alumno.id && it.estadoPago == EstadoPago.PENDIENTE
                    } ?: continue
                    val fechaStr = with(DateUtils) { claseConAlumnos.clase.fecha.toLocalDate().toString() }
                    canvas.drawText(
                        "  Fecha: $fechaStr  ${claseConAlumnos.clase.horaInicio}–${claseConAlumnos.clase.horaFin}  ${formatPrice(ref.precioIndividual)}",
                        left, y, paint
                    )
                    y += 18f
                }
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
                canvas.drawText("  → Subtotal: ${formatPrice(deudor.montoPendiente)}", left, y, paint)
                y += 26f
            }
        } else {
            canvas.drawText("¡Todo cobrado! No hay deudores.", left, y, paint)
        }

        document.finishPage(page)

        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                document.writeTo(outputStream)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            document.close()
        }
    }
}
