package com.powerplant.firesurvey.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import com.powerplant.firesurvey.data.Extinguisher
import com.powerplant.firesurvey.data.Inspection
import java.io.File
import java.time.LocalDate

object Sharing {

    private fun sharedDir(context: Context): File =
        File(context.cacheDir, "shared").apply { mkdirs() }

    private fun share(context: Context, file: File, mime: String, title: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun shareLabel(context: Context, extinguisher: Extinguisher) {
        val bitmap = QrGenerator.labelBitmap(extinguisher.id, extinguisher.name, extinguisher.location)
        val safeName = extinguisher.id.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val file = File(sharedDir(context), "qr_$safeName.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        share(context, file, "image/png", "QR label – ${extinguisher.name}")
    }

    fun shareInspectionsCsv(context: Context, extinguishers: List<Extinguisher>, inspections: List<Inspection>) {
        val byId = extinguishers.associateBy { it.id }
        val csv = buildString {
            appendLine("Date,Extinguisher code,Name,Location,Type,Inspector,Result,Conditions,Notes")
            for (i in inspections) {
                val e = byId[i.extinguisherId]
                val row = listOf(
                    Formats.dateTime(i.timestamp),
                    i.extinguisherId,
                    e?.name.orEmpty(),
                    e?.location.orEmpty(),
                    e?.type.orEmpty(),
                    i.inspector,
                    i.statusEnum.label,
                    i.issueList.joinToString("; ") { it.label },
                    i.notes,
                )
                appendLine(row.joinToString(",") { csvField(it) })
            }
        }
        val file = File(sharedDir(context), "extinguisher_survey_${LocalDate.now()}.csv")
        // BOM so Excel opens UTF-8 (e.g. Arabic names) correctly
        file.writeText("﻿" + csv)
        share(context, file, "text/csv", "Extinguisher survey export")
    }

    private fun csvField(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + value.replace("\"", "\"\"") + "\"" else value
}
