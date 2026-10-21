package com.yc.navigator.data

import kotlinx.serialization.Serializable

@Serializable
data class DocumentNode(
    val id: String,
    val name: String,
    val isFolder: Boolean,
    val storedFileName: String? = null,
    val children: List<DocumentNode> = emptyList()
)
