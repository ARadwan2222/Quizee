package com.example.vocards.ui

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.vocards.data.Project
import com.example.vocards.data.Vocad
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object FileExportHelper {
    fun exportProject(context: Context, project: Project, vocads: List<Vocad>) {
        val json = JSONObject().apply {
            put("projectName", project.name)
            put("description", project.description)
            val array = JSONArray()
            vocads.forEach { vocad ->
                array.put(JSONObject().apply {
                    put("word", vocad.word)
                    put("definition", vocad.definition)
                    put("example", vocad.example)
                })
            }
            put("words", array)
        }

        val fileName = "${project.name.replace(" ", "_")}_vocads.json"
        val tempFile = File(context.cacheDir, fileName)
        tempFile.writeText(json.toString())

        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Project"))
    }
}
