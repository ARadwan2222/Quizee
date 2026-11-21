package com.example.vocards.ui

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import org.zwobble.mammoth.DocumentConverter
import java.io.BufferedReader
import java.io.InputStreamReader

object FileImportHelper {
    
    fun extractText(context: Context, uri: Uri): String? {
        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(uri)
        val fileName = contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex != -1) cursor.getString(nameIndex) else null
        } ?: uri.lastPathSegment ?: ""

        val isPdf = mimeType == "application/pdf" || fileName.endsWith(".pdf", ignoreCase = true)
        val isDocx = mimeType == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" ||
                mimeType == "application/msword" ||
                fileName.endsWith(".docx", ignoreCase = true) ||
                fileName.endsWith(".doc", ignoreCase = true)

        return try {
            when {
                isPdf -> {
                    PDFBoxResourceLoader.init(context)
                    contentResolver.openInputStream(uri).use { inputStream ->
                        PDDocument.load(inputStream).use { document ->
                            PDFTextStripper().getText(document)
                        }
                    }
                }
                isDocx -> {
                    contentResolver.openInputStream(uri).use { inputStream ->
                        val converter = DocumentConverter()
                        val result = converter.extractRawText(inputStream)
                        result.value
                    }
                }
                else -> { // Default to text/plain
                    contentResolver.openInputStream(uri).use { inputStream ->
                        BufferedReader(InputStreamReader(inputStream)).use { reader ->
                            reader.readText()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
