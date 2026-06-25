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
        
        return try {
            when {
                mimeType == "application/pdf" -> {
                    PDFBoxResourceLoader.init(context)
                    contentResolver.openInputStream(uri).use { inputStream ->
                        PDDocument.load(inputStream).use { document ->
                            PDFTextStripper().getText(document)
                        }
                    }
                }
                mimeType == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> {
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
