package com.yc.navigator.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yc.navigator.R
import com.yc.navigator.data.DocumentNode
import com.yc.navigator.ui.NavigatorViewModel
import com.yc.navigator.ui.components.PdfPagesViewer
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun DocumentsScreen(
    documents: List<DocumentNode>,
    snackbarHost: SnackbarHostState,
    documentFile: (String) -> File?,
    documentParentId: (String) -> String?,
    documentFolderDepth: (String) -> Int?,
    onAddFolder: (String?, String) -> DocumentNode?,
    onRename: (String, String) -> Boolean,
    onDelete: (String) -> Unit,
    onImportDocument: suspend (String, Uri, String) -> DocumentNode?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val selectFolderForUpload = stringResource(R.string.documents_select_folder_for_upload)
    val maxDepthMessage = stringResource(R.string.documents_max_depth)
    val uploadSuccess = stringResource(R.string.documents_upload_success)
    val uploadError = stringResource(R.string.documents_upload_error)

    var selectedFolderId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedFileId by rememberSaveable { mutableStateOf<String?>(null) }
    var expandedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var uploadFolderId by remember { mutableStateOf<String?>(null) }

    var folderDialogVisible by remember { mutableStateOf(false) }
    var folderDialogParentId by remember { mutableStateOf<String?>(null) }
    var folderDialogName by remember { mutableStateOf("") }
    var renameTarget by remember { mutableStateOf<DocumentNode?>(null) }
    var renameName by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<DocumentNode?>(null) }

    val currentFolder = selectedFolderId
        ?.let { findDocumentNode(documents, it) }
        ?.takeIf { it.isFolder }
    val currentFile = selectedFileId
        ?.let { findDocumentNode(documents, it) }
        ?.takeIf { !it.isFolder }

    LaunchedEffect(documents) {
        if (selectedFolderId != null && findDocumentNode(documents, selectedFolderId!!) == null) {
            selectedFolderId = null
        }
        if (selectedFileId != null && findDocumentNode(documents, selectedFileId!!) == null) {
            selectedFileId = null
        }
    }

    val addFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        val folderId = uploadFolderId
        uploadFolderId = null
        if (uri != null && folderId != null) {
            val displayName = queryDisplayName(context, uri)
            scope.launch {
                val node = onImportDocument(
                    folderId,
                    uri,
                    displayName ?: "document.pdf"
                )
                if (node != null) {
                    selectedFolderId = folderId
                    selectedFileId = node.id
                    expandedIds = expandedIds + folderId
                    snackbarHost.showSnackbar(uploadSuccess)
                } else {
                    snackbarHost.showSnackbar(uploadError)
                }
            }
        }
    }

    fun requestUpload() {
        val folder = currentFolder
        if (folder == null) {
            scope.launch { snackbarHost.showSnackbar(selectFolderForUpload) }
            return
        }
        uploadFolderId = folder.id
        addFileLauncher.launch(arrayOf("application/pdf"))
    }

    fun requestAddFolder() {
        val parentId = selectedFolderId
        if (parentId != null) {
            val depth = documentFolderDepth(parentId)
            if (depth == null || depth >= NavigatorViewModel.MAX_DOCUMENT_FOLDER_DEPTH) {
                scope.launch { snackbarHost.showSnackbar(maxDepthMessage) }
                return
            }
        }
        folderDialogParentId = parentId
        folderDialogName = ""
        folderDialogVisible = true
    }

    fun toggleFolder(node: DocumentNode) {
        selectedFolderId = node.id
        selectedFileId = null
        expandedIds = if (node.id in expandedIds) {
            expandedIds - node.id
        } else {
            expandedIds + node.id
        }
    }

    fun openFile(node: DocumentNode) {
        selectedFileId = node.id
        selectedFolderId = documentParentId(node.id)
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val compact = maxWidth < 760.dp
        val drawerState = rememberDrawerState(DrawerValue.Closed)

        if (compact) {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet {
                        DocumentsSidebar(
                            documents = documents,
                            selectedFolderId = selectedFolderId,
                            selectedFileId = selectedFileId,
                            expandedIds = expandedIds,
                            modifier = Modifier
                                .width(320.dp)
                                .fillMaxHeight(),
                            onFolderClick = ::toggleFolder,
                            onFileClick = { node ->
                                openFile(node)
                                scope.launch { drawerState.close() }
                            },
                            onAddFolder = ::requestAddFolder,
                            onRename = { node ->
                                renameTarget = node
                                renameName = node.name
                            },
                            onDelete = { deleteTarget = it },
                            onClearSelection = {
                                selectedFolderId = null
                                selectedFileId = null
                            }
                        )
                    }
                }
            ) {
                DocumentsContent(
                    currentFolder = currentFolder,
                    currentFile = currentFile,
                    currentFileFile = currentFile?.let { documentFile(it.id) },
                    compact = true,
                    onOpenFolders = { scope.launch { drawerState.open() } },
                    onAddFile = ::requestUpload,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            Row(Modifier.fillMaxSize()) {
                DocumentsSidebar(
                    documents = documents,
                    selectedFolderId = selectedFolderId,
                    selectedFileId = selectedFileId,
                    expandedIds = expandedIds,
                    modifier = Modifier
                        .width(300.dp)
                        .fillMaxHeight(),
                    onFolderClick = ::toggleFolder,
                    onFileClick = ::openFile,
                    onAddFolder = ::requestAddFolder,
                    onRename = { node ->
                        renameTarget = node
                        renameName = node.name
                    },
                    onDelete = { deleteTarget = it },
                    onClearSelection = {
                        selectedFolderId = null
                        selectedFileId = null
                    }
                )
                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                DocumentsContent(
                    currentFolder = currentFolder,
                    currentFile = currentFile,
                    currentFileFile = currentFile?.let { documentFile(it.id) },
                    compact = false,
                    onOpenFolders = {},
                    onAddFile = ::requestUpload,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
        }
    }

    if (folderDialogVisible) {
        AlertDialog(
            onDismissRequest = { folderDialogVisible = false },
            title = { Text(stringResource(R.string.documents_new_folder)) },
            text = {
                OutlinedTextField(
                    value = folderDialogName,
                    onValueChange = { folderDialogName = it },
                    label = { Text(stringResource(R.string.documents_name)) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    enabled = folderDialogName.isNotBlank(),
                    onClick = {
                        val parentId = folderDialogParentId
                        val node = onAddFolder(parentId, folderDialogName)
                        if (node != null) {
                            folderDialogVisible = false
                            if (parentId != null) {
                                expandedIds = expandedIds + parentId
                            }
                            selectedFolderId = node.id
                            selectedFileId = null
                        } else {
                            scope.launch { snackbarHost.showSnackbar(maxDepthMessage) }
                        }
                    }
                ) {
                    Text(stringResource(R.string.action_create))
                }
            },
            dismissButton = {
                TextButton(onClick = { folderDialogVisible = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    renameTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text(stringResource(R.string.action_rename)) },
            text = {
                OutlinedTextField(
                    value = renameName,
                    onValueChange = { renameName = it },
                    label = { Text(stringResource(R.string.documents_name)) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    enabled = renameName.isNotBlank(),
                    onClick = {
                        if (onRename(target.id, renameName)) {
                            renameTarget = null
                        }
                    }
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = {
                Text(
                    stringResource(
                        if (target.isFolder) {
                            R.string.documents_delete_folder_title
                        } else {
                            R.string.documents_delete_file_title
                        }
                    )
                )
            },
            text = {
                Text(
                    stringResource(
                        if (target.isFolder) {
                            R.string.documents_delete_folder_text
                        } else {
                            R.string.documents_delete_file_text
                        }
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val ids = collectSubtreeIds(target)
                    selectedFolderId?.let { if (it in ids) selectedFolderId = null }
                    selectedFileId?.let { if (it in ids) selectedFileId = null }
                    deleteTarget = null
                    onDelete(target.id)
                }) {
                    Text(
                        stringResource(R.string.action_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun DocumentsSidebar(
    documents: List<DocumentNode>,
    selectedFolderId: String?,
    selectedFileId: String?,
    expandedIds: Set<String>,
    onFolderClick: (DocumentNode) -> Unit,
    onFileClick: (DocumentNode) -> Unit,
    onAddFolder: () -> Unit,
    onRename: (DocumentNode) -> Unit,
    onDelete: (DocumentNode) -> Unit,
    onClearSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.background(MaterialTheme.colorScheme.surface)
    ) {
        Text(
            text = stringResource(R.string.documents_folders),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClearSelection
                )
                .padding(vertical = 8.dp)
        ) {
            if (documents.isEmpty()) {
                Text(
                    text = stringResource(R.string.documents_no_folders),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            documents.forEach { node ->
                DocumentTreeRow(
                    node = node,
                    depth = 1,
                    selectedFolderId = selectedFolderId,
                    selectedFileId = selectedFileId,
                    expandedIds = expandedIds,
                    onFolderClick = onFolderClick,
                    onFileClick = onFileClick,
                    onRename = onRename,
                    onDelete = onDelete
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        TextButton(
            onClick = onAddFolder,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.documents_add_folder))
        }
    }
}

@Composable
private fun DocumentTreeRow(
    node: DocumentNode,
    depth: Int,
    selectedFolderId: String?,
    selectedFileId: String?,
    expandedIds: Set<String>,
    onFolderClick: (DocumentNode) -> Unit,
    onFileClick: (DocumentNode) -> Unit,
    onRename: (DocumentNode) -> Unit,
    onDelete: (DocumentNode) -> Unit
) {
    val expanded = node.id in expandedIds
    val selected = if (node.isFolder) {
        selectedFileId == null && node.id == selectedFolderId
    } else {
        node.id == selectedFileId
    }
    var menuOpen by remember(node.id) { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (selected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    } else {
                        Color.Transparent
                    }
                )
                .clickable {
                    if (node.isFolder) {
                        onFolderClick(node)
                    } else {
                        onFileClick(node)
                    }
                }
                .padding(
                    start = ((depth - 1) * 14).dp + 8.dp,
                    end = 4.dp,
                    top = 5.dp,
                    bottom = 5.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (node.isFolder) {
                Icon(
                    imageVector = if (expanded) {
                        Icons.Filled.KeyboardArrowDown
                    } else {
                        Icons.AutoMirrored.Filled.KeyboardArrowRight
                    },
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Spacer(Modifier.width(18.dp))
            }
            Icon(
                painter = painterResource(
                    if (node.isFolder) R.drawable.ic_folder else R.drawable.ic_pdf_file
                ),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (node.isFolder) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = node.name,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Box {
                IconButton(
                    onClick = { menuOpen = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.action_more)
                    )
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_rename)) },
                        onClick = {
                            menuOpen = false
                            onRename(node)
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(R.string.action_delete),
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            menuOpen = false
                            onDelete(node)
                        }
                    )
                }
            }
        }

        if (node.isFolder && expanded) {
            node.children.forEach { child ->
                DocumentTreeRow(
                    node = child,
                    depth = depth + 1,
                    selectedFolderId = selectedFolderId,
                    selectedFileId = selectedFileId,
                    expandedIds = expandedIds,
                    onFolderClick = onFolderClick,
                    onFileClick = onFileClick,
                    onRename = onRename,
                    onDelete = onDelete
                )
            }
        }
    }
}

@Composable
private fun DocumentsContent(
    currentFolder: DocumentNode?,
    currentFile: DocumentNode?,
    currentFileFile: File?,
    compact: Boolean,
    onOpenFolders: () -> Unit,
    onAddFile: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (compact) {
                IconButton(onClick = onOpenFolders) {
                    Icon(
                        Icons.Filled.Menu,
                        contentDescription = stringResource(R.string.documents_open_folders)
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = currentFile?.name
                        ?: currentFolder?.name
                        ?: stringResource(R.string.documents_title),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (currentFile != null && currentFolder != null) {
                    Text(
                        text = currentFolder.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Button(onClick = onAddFile) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.documents_add))
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when {
                currentFile != null -> {
                    if (currentFileFile != null && currentFileFile.exists()) {
                        PdfPagesViewer(
                            file = currentFileFile,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        DocumentsEmptyState(
                            text = stringResource(R.string.documents_file_missing),
                            hint = null
                        )
                    }
                }

                currentFolder != null -> DocumentsEmptyState(
                    text = stringResource(R.string.documents_empty_folder),
                    hint = stringResource(R.string.documents_empty_folder_hint)
                )

                else -> DocumentsEmptyState(
                    text = stringResource(R.string.documents_select_folder),
                    hint = stringResource(R.string.documents_select_folder_hint)
                )
            }
        }
    }
}

@Composable
private fun DocumentsEmptyState(
    text: String,
    hint: String?
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (hint != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
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

private fun collectSubtreeIds(node: DocumentNode): Set<String> {
    val ids = mutableSetOf<String>()
    fun collect(current: DocumentNode) {
        ids += current.id
        current.children.forEach(::collect)
    }
    collect(node)
    return ids
}

private fun queryDisplayName(context: Context, uri: Uri): String? = runCatching {
    context.contentResolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME),
        null,
        null,
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0) cursor.getString(index) else null
        } else {
            null
        }
    }
}.getOrNull()
