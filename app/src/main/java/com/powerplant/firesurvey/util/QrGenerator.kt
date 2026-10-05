package com.powerplant.firesurvey.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

object QrGenerator {

    fun qrBitmap(content: String, size: Int = 600): Bitmap {
        val hints = mapOf(
            EncodeHintType.MARGIN to 1,
            EncodeHintType.CHARACTER_SET to "UTF-8",
            // High error correction: labels in a plant get dirty and scratched.
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
        )
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val pixels = IntArray(size * size) { i -> if (matrix[i % size, i / size]) Color.BLACK else Color.WHITE }
        return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
    }

    /** A printable label: QR code with the extinguisher name and code underneath. */
    fun labelBitmap(code: String, name: String, location: String): Bitmap {
        val qrSize = 600
        val padding = 40
        val qr = qrBitmap(code, qrSize)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textAlign = Paint.Align.CENTER
        }
        val lines = listOf(
            Triple(name, 44f, Typeface.BOLD),
            Triple(code, 36f, Typeface.NORMAL),
            Triple(location, 32f, Typeface.NORMAL),
        ).filter { it.first.isNotBlank() }
        val textHeight = lines.sumOf { (it.second * 1.4f).toInt() }
        val width = qrSize + padding * 2
        val height = qrSize + padding * 2 + textHeight
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(qr, padding.toFloat(), padding.toFloat(), null)
        var y = (padding + qrSize).toFloat()
        for ((text, size, style) in lines) {
            textPaint.textSize = size
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, style)
            y += size * 1.4f
            canvas.drawText(ellipsize(text, textPaint, width - padding * 2f), width / 2f, y - size * 0.3f, textPaint)
        }
        return bitmap
    }

    private fun ellipsize(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var end = text.length
        while (end > 0 && paint.measureText(text.substring(0, end) + "…") > maxWidth) end--
        return text.substring(0, end) + "…"
    }
}
