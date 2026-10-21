package com.yc.navigator.data

import android.content.Context
import android.content.res.Configuration
import android.util.Log
import com.yc.navigator.R
import java.io.File
import java.util.Locale
import java.util.UUID
import kotlinx.serialization.json.Json

class DocumentsRepository(context: Context) {

    private val appContext = context.applicationContext
    private val documentsDir = File(appContext.filesDir, "documents").apply { mkdirs() }
    private val filesDir = File(documentsDir, "files").apply { mkdirs() }
    private val treeFile = File(documentsDir, "tree.json")

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun load(): List<DocumentNode> {
        val nodes = if (!treeFile.exists()) {
            defaultNodes().also(::save)
        } else {
            runCatching {
                json.decodeFromString<List<DocumentNode>>(treeFile.readText(Charsets.UTF_8))
            }.getOrElse { error ->
                Log.e("DocumentsRepository", "Failed to read documents tree", error)
                emptyList()
            }
        }
        return migrateDefaultNames(nodes)
    }

    fun save(nodes: List<DocumentNode>) {
        val tmp = File(treeFile.parentFile, treeFile.name + ".tmp")
        tmp.writeText(json.encodeToString(nodes), Charsets.UTF_8)
        if (!tmp.renameTo(treeFile)) {
            treeFile.writeText(json.encodeToString(nodes), Charsets.UTF_8)
            tmp.delete()
        }
    }

    fun createStoredFileName(): String = "${UUID.randomUUID()}.pdf"

    fun fileFor(storedFileName: String): File = File(filesDir, storedFileName)

    fun deleteStoredFiles(storedFileNames: Collection<String>) {
        storedFileNames.forEach { name ->
            runCatching { fileFor(name).delete() }
        }
    }

    private fun defaultNodes(): List<DocumentNode> {
        val context = localizedContext()
        return listOf(
            DocumentNode(
                id = UUID.randomUUID().toString(),
                name = context.getString(R.string.documents_default_laws),
                isFolder = true
            ),
            DocumentNode(
                id = UUID.randomUUID().toString(),
                name = context.getString(R.string.documents_default_manual),
                isFolder = true
            ),
            DocumentNode(
                id = UUID.randomUUID().toString(),
                name = context.getString(R.string.documents_default_aip),
                isFolder = true
            )
        )
    }

    private fun migrateDefaultNames(nodes: List<DocumentNode>): List<DocumentNode> {
        val context = localizedContext()
        val laws = context.getString(R.string.documents_default_laws)
        val manual = context.getString(R.string.documents_default_manual)
        var changed = false
        val migrated = nodes.map { node ->
            if (!node.isFolder) return@map node
            when (node.name) {
                "Laws" -> {
                    changed = true
                    node.copy(name = laws)
                }
                "Flight Manual" -> {
                    changed = true
                    node.copy(name = manual)
                }
                else -> node
            }
        }
        if (changed) save(migrated)
        return migrated
    }

    private fun localizedContext(): Context {
        val languageTag = SettingsStore(appContext).load().languageTag
        val locale = Locale.forLanguageTag(if (languageTag == "en") "en" else "ru-RU")
        val configuration = Configuration(appContext.resources.configuration).apply {
            setLocale(locale)
        }
        return appContext.createConfigurationContext(configuration)
    }
}
