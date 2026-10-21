package com.yc.navigator.export

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

object ShareUtils {

    fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri(file.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(intent, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun sharedFile(context: Context, fileName: String): File {
        val dir = File(context.cacheDir, "shared").apply {
            mkdirs()
            // Clean up old files (older than 24 hours)
            listFiles()?.filter {
                System.currentTimeMillis() - it.lastModified() > 24 * 60 * 60 * 1000
            }?.forEach { file ->
                file.delete()
            }
        }
        return File(dir, sanitizeFileName(fileName))
    }

    fun sanitizeFileName(name: String): String =
        name.replace(Regex("[\\\\/:*?\"<>|]"), "_").take(120)
}
