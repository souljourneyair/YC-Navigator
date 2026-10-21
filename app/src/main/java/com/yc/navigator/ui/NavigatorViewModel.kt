package com.yc.navigator.ui

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yc.navigator.data.AircraftTypesStore
import com.yc.navigator.data.AppSettings
import com.yc.navigator.data.DocumentNode
import com.yc.navigator.data.DocumentsRepository
import com.yc.navigator.data.IdGenerator
import com.yc.navigator.data.ImportException
import com.yc.navigator.data.Journal
import com.yc.navigator.data.Leg
import com.yc.navigator.data.NavigatorRepository
import com.yc.navigator.data.Pilot
import com.yc.navigator.data.SettingsStore
import com.yc.navigator.data.ThemeMode
import com.yc.navigator.data.UnitMode
import com.yc.navigator.data.WeatherReportType
import com.yc.navigator.data.WeatherStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDate
import java.util.UUID

class NavigatorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = NavigatorRepository(application)
    private val documentsRepository = DocumentsRepository(application)
    private val settingsStore = SettingsStore(application)
    private val aircraftTypesStore = AircraftTypesStore(application)
    private val weatherStore = WeatherStore(application)
    private val idGenerator = IdGenerator(application)
    private val saveDispatcher = Dispatchers.IO.limitedParallelism(1)
    private val weatherJson = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    var journals by mutableStateOf(repository.loadJournals())
        private set

    var pilots by mutableStateOf(repository.loadPilots())
        private set

    var settings by mutableStateOf(settingsStore.load())
        private set

    var documentItems by mutableStateOf(documentsRepository.load())
        private set

    var aircraftTypes by mutableStateOf(aircraftTypesStore.load())
        private set

    var weatherReports by mutableStateOf(weatherStore.load())
        private set

    init {
        val maxExistingId = (
            journals.map { it.id } +
                journals.flatMap { journal -> journal.legs.map { it.id } } +
                pilots.map { it.id }
            ).maxOrNull() ?: 0L
        idGenerator.updateMaxId(maxExistingId)

        if (!aircraftTypesStore.isInitialized()) {
            val seeded = journals
                .mapNotNull { it.aircraftType.trim().takeIf(String::isNotEmpty) }
                .distinctBy { it.lowercase() }
            aircraftTypes = seeded
            aircraftTypesStore.save(seeded)
        }
    }

    fun journalById(id: Long): Journal? = journals.firstOrNull { it.id == id }

    fun pilotById(id: Long?): Pilot? = pilots.firstOrNull { it.id == id }

    fun sortedJournals(): List<Journal> = journals.sortedByDescending { it.updatedAt }

    fun searchJournals(query: String): List<Journal> {
        val clean = query.trim()
        if (clean.isEmpty()) return sortedJournals()
        val lower = clean.lowercase()
        return sortedJournals().filter { journal ->
            journal.number.lowercase().contains(lower) ||
                journal.unit.lowercase().contains(lower) ||
                journal.aircraftType.lowercase().contains(lower) ||
                journal.aircraftNumber.lowercase().contains(lower) ||
                journal.departure.name.lowercase().contains(lower) ||
                journal.arrival.name.lowercase().contains(lower) ||
                journal.legs.any { it.route.lowercase().contains(lower) } ||
                pilotName(journal.pilots.commanderId).lowercase().contains(lower) ||
                pilotName(journal.pilots.secondPilotId).lowercase().contains(lower)
        }
    }

    fun createJournal(): Journal {
        val last = journals.maxByOrNull { it.updatedAt }
        val journal = Journal(
            id = idGenerator.nextId(),
            number = nextNumber(),
            dateIso = LocalDate.now().toString(),
            unit = last?.unit ?: "",
            aircraftType = last?.aircraftType ?: "",
            aircraftNumber = last?.aircraftNumber ?: "",
            legs = listOf(Leg(id = idGenerator.nextId())),
            preflightDateIso = LocalDate.now().toString(),
            updatedAt = System.currentTimeMillis()
        )
        journals = journals + journal
        persistJournals()
        return journal
    }

    fun updateJournal(journal: Journal) {
        val updated = journal.copy(updatedAt = System.currentTimeMillis())
        journals = journals.map { if (it.id == updated.id) updated else it }
        persistJournals()
    }

    fun deleteJournal(id: Long) {
        journals = journals.filterNot { it.id == id }
        persistJournals()
    }

    fun duplicateJournal(id: Long): Journal? {
        val source = journalById(id) ?: return null
        val copy = source.copy(
            id = idGenerator.nextId(),
            number = nextNumber(),
            dateIso = LocalDate.now().toString(),
            legs = source.legs.map { it.copy(id = idGenerator.nextId()) },
            sunEntries = source.sunEntries.map { it.copy(id = idGenerator.nextId()) },
            updatedAt = System.currentTimeMillis()
        )
        journals = journals + copy
        persistJournals()
        return copy
    }

    fun addPilot(name: String): Pilot? {
        val clean = name.trim()
        if (clean.isEmpty() || pilots.size >= MAX_PILOTS) return null
        val pilot = Pilot(id = idGenerator.nextId(), name = clean)
        pilots = pilots + pilot
        persistPilots()
        return pilot
    }

    fun updatePilot(pilot: Pilot) {
        val clean = pilot.copy(name = pilot.name.trim())
        if (clean.name.isEmpty()) return
        pilots = pilots.map { if (it.id == clean.id) clean else it }
        persistPilots()
    }

    fun deletePilot(id: Long) {
        pilots = pilots.filterNot { it.id == id }
        journals = journals.map { journal ->
            journal.copy(
                pilots = journal.pilots.copy(
                    commanderId = journal.pilots.commanderId.takeUnless { it == id },
                    secondPilotId = journal.pilots.secondPilotId.takeUnless { it == id },
                    dutyNavigatorId = journal.pilots.dutyNavigatorId.takeUnless { it == id },
                    squadronNavigatorId = journal.pilots.squadronNavigatorId.takeUnless { it == id }
                )
            )
        }
        persistPilots()
        persistJournals()
    }

    fun journalsUsingPilot(id: Long): Int = journals.count { journal ->
        journal.pilots.commanderId == id ||
            journal.pilots.secondPilotId == id ||
            journal.pilots.dutyNavigatorId == id ||
            journal.pilots.squadronNavigatorId == id
    }

    fun setLanguage(tag: String) {
        settings = settings.copy(languageTag = tag)
        settingsStore.save(settings)
    }

    fun setUnitMode(mode: UnitMode) {
        settings = settings.copy(unitMode = mode)
        settingsStore.save(settings)
    }

    fun setThemeMode(mode: ThemeMode) {
        settings = settings.copy(themeMode = mode)
        settingsStore.save(settings)
    }

    fun exportJson(): String = repository.exportBundle(journals, pilots, settings)

    fun importJson(raw: String) {
        val bundle = repository.importBundle(raw)
        if (bundle.pilots.size > MAX_PILOTS) {
            throw ImportException("Import contains more than $MAX_PILOTS pilots")
        }
        journals = bundle.journals
        pilots = bundle.pilots
        settings = bundle.settings
        val maxImportedId = (
            bundle.journals.map { it.id } +
                bundle.journals.flatMap { journal -> journal.legs.map { it.id } } +
                bundle.pilots.map { it.id }
            ).maxOrNull() ?: 0L
        idGenerator.updateMaxId(maxImportedId)
        persistJournals()
        persistPilots()
        settingsStore.save(settings)
    }

    fun saveAircraftType(value: String) {
        val clean = value.trim()
        if (clean.isEmpty()) return
        val updated = (listOf(clean) + aircraftTypes.filterNot {
            it.equals(clean, ignoreCase = true)
        }).take(MAX_AIRCRAFT_TYPES)
        if (updated != aircraftTypes) {
            aircraftTypes = updated
            aircraftTypesStore.save(updated)
        }
    }

    fun removeAircraftType(value: String) {
        val updated = aircraftTypes.filterNot { it.equals(value, ignoreCase = true) }
        if (updated != aircraftTypes) {
            aircraftTypes = updated
            aircraftTypesStore.save(updated)
        }
    }

    fun setWeatherReport(type: String, text: String) {
        if (WeatherReportType.fromKey(type) == null) return
        weatherReports = weatherReports + (type to text)
        persistWeather()
    }

    fun importWeatherReports(raw: String): Boolean {
        val parsed = runCatching {
            weatherJson.decodeFromString<Map<String, String>>(raw)
        }.getOrNull() ?: return false
        val known = parsed.filterKeys { WeatherReportType.fromKey(it) != null }
        val legacyCombined = listOf(
            parsed[WeatherReportType.METAR_TAF.key],
            parsed["METAR"],
            parsed["TAF"]
        ).filterNotNull()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n")
        val imported = if (legacyCombined.isNotBlank() && parsed["METAR/TAF"].isNullOrBlank()) {
            known + (WeatherReportType.METAR_TAF.key to legacyCombined)
        } else {
            known
        }
        if (imported.isEmpty()) return false
        weatherReports = weatherReports + imported
        persistWeather()
        return true
    }

    fun exportWeatherJson(): String {
        val ordered = linkedMapOf<String, String>()
        WeatherReportType.entries.forEach { type ->
            ordered[type.key] = weatherReports[type.key].orEmpty()
        }
        return weatherJson.encodeToString(ordered)
    }

    fun documentFile(nodeId: String): File? {
        val node = findDocumentNode(documentItems, nodeId) ?: return null
        if (node.isFolder) return null
        return node.storedFileName?.let(documentsRepository::fileFor)
    }

    fun documentParentId(nodeId: String): String? =
        findDocumentParentId(documentItems, nodeId)

    fun documentFolderDepth(nodeId: String): Int? =
        findFolderDepth(documentItems, nodeId)

    fun addDocumentFolder(parentId: String?, name: String): DocumentNode? {
        val clean = name.trim()
        if (clean.isEmpty()) return null
        if (parentId != null) {
            val parent = findDocumentNode(documentItems, parentId)
            if (parent == null || !parent.isFolder) return null
            val depth = findFolderDepth(documentItems, parentId) ?: return null
            if (depth >= MAX_DOCUMENT_FOLDER_DEPTH) return null
        }
        val node = DocumentNode(
            id = UUID.randomUUID().toString(),
            name = clean,
            isFolder = true
        )
        documentItems = if (parentId == null) {
            documentItems + node
        } else {
            addDocumentChild(documentItems, parentId, node)
        }
        persistDocuments()
        return node
    }

    fun renameDocumentNode(nodeId: String, name: String): Boolean {
        val clean = name.trim()
        if (clean.isEmpty() || findDocumentNode(documentItems, nodeId) == null) return false
        documentItems = updateDocumentNode(documentItems, nodeId) { node ->
            node.copy(name = clean)
        }
        persistDocuments()
        return true
    }

    fun deleteDocumentNode(nodeId: String) {
        val node = findDocumentNode(documentItems, nodeId) ?: return
        val storedFiles = collectStoredFiles(node)
        documentItems = removeDocumentNode(documentItems, nodeId)
        persistDocuments()
        viewModelScope.launch(Dispatchers.IO) {
            documentsRepository.deleteStoredFiles(storedFiles)
        }
    }

    suspend fun importDocument(
        folderId: String,
        uri: Uri,
        displayName: String
    ): DocumentNode? {
        val folder = findDocumentNode(documentItems, folderId)
        if (folder == null || !folder.isFolder) return null

        val storedFileName = documentsRepository.createStoredFileName()
        val target = documentsRepository.fileFor(storedFileName)
        val copied = withContext(Dispatchers.IO) {
            copyPdfToStorage(uri, target)
        }
        if (!copied) {
            target.delete()
            return null
        }

        val node = DocumentNode(
            id = UUID.randomUUID().toString(),
            name = normalizeDocumentName(displayName),
            isFolder = false,
            storedFileName = storedFileName
        )
        documentItems = addDocumentChild(documentItems, folderId, node)
        persistDocuments()
        return node
    }

    private fun copyPdfToStorage(uri: Uri, target: File): Boolean = runCatching {
        val input = getApplication<Application>().contentResolver.openInputStream(uri)
            ?: error("Cannot open document")
        input.use { stream ->
            val header = ByteArray(5)
            val read = stream.read(header)
            if (read != header.size || String(header, 0, read, Charsets.US_ASCII) != "%PDF-") {
                error("Not a PDF document")
            }
            target.outputStream().use { output ->
                output.write(header, 0, read)
                stream.copyTo(output)
            }
        }
    }.isSuccess

    private fun normalizeDocumentName(raw: String): String {
        val base = raw.trim()
            .replace(Regex("[\\p{Cntrl}]"), "")
            .take(116)
            .ifBlank { "document" }
        return if (base.endsWith(".pdf", ignoreCase = true)) base else "$base.pdf"
    }

    private fun persistDocuments() {
        val snapshot = documentItems
        viewModelScope.launch(saveDispatcher) {
            documentsRepository.save(snapshot)
        }
    }

    private fun persistWeather() {
        val snapshot = weatherReports
        viewModelScope.launch(saveDispatcher) {
            weatherStore.save(snapshot)
        }
    }

    private fun findDocumentNode(nodes: List<DocumentNode>, id: String): DocumentNode? {
        nodes.forEach { node ->
            if (node.id == id) return node
            if (node.isFolder) {
                findDocumentNode(node.children, id)?.let { return it }
            }
        }
        return null
    }

    private fun findDocumentParentId(
        nodes: List<DocumentNode>,
        id: String,
        parentId: String? = null
    ): String? {
        nodes.forEach { node ->
            if (node.id == id) return parentId
            if (node.isFolder) {
                findDocumentParentId(node.children, id, node.id)?.let { return it }
            }
        }
        return null
    }

    private fun findFolderDepth(
        nodes: List<DocumentNode>,
        id: String,
        level: Int = 1
    ): Int? {
        nodes.forEach { node ->
            if (node.id == id) return if (node.isFolder) level else null
            if (node.isFolder) {
                findFolderDepth(node.children, id, level + 1)?.let { return it }
            }
        }
        return null
    }

    private fun addDocumentChild(
        nodes: List<DocumentNode>,
        parentId: String,
        child: DocumentNode
    ): List<DocumentNode> = nodes.map { node ->
        when {
            node.id == parentId && node.isFolder ->
                node.copy(children = node.children + child)
            node.isFolder ->
                node.copy(children = addDocumentChild(node.children, parentId, child))
            else -> node
        }
    }

    private fun updateDocumentNode(
        nodes: List<DocumentNode>,
        id: String,
        transform: (DocumentNode) -> DocumentNode
    ): List<DocumentNode> = nodes.map { node ->
        when {
            node.id == id -> transform(node)
            node.isFolder ->
                node.copy(children = updateDocumentNode(node.children, id, transform))
            else -> node
        }
    }

    private fun removeDocumentNode(
        nodes: List<DocumentNode>,
        id: String
    ): List<DocumentNode> = nodes
        .filterNot { it.id == id }
        .map { node ->
            if (node.isFolder) {
                node.copy(children = removeDocumentNode(node.children, id))
            } else {
                node
            }
        }

    private fun collectStoredFiles(node: DocumentNode): List<String> {
        val files = mutableListOf<String>()
        fun collect(current: DocumentNode) {
            current.storedFileName?.let(files::add)
            current.children.forEach(::collect)
        }
        collect(node)
        return files
    }

    private fun pilotName(id: Long?): String = pilotById(id)?.name ?: ""

    private fun nextNumber(): String {
        val maxNumber = journals.mapNotNull { it.number.toIntOrNull() }.maxOrNull() ?: 0
        return (maxNumber + 1).toString()
    }

    private fun persistJournals() {
        val snapshot = journals
        viewModelScope.launch(saveDispatcher) {
            repository.saveJournals(snapshot)
        }
    }

    private fun persistPilots() {
        val snapshot = pilots
        viewModelScope.launch(saveDispatcher) {
            repository.savePilots(snapshot)
        }
    }

    companion object {
        const val MAX_PILOTS = 10
        const val MAX_DOCUMENT_FOLDER_DEPTH = 3
        const val MAX_AIRCRAFT_TYPES = 30
    }
}
