package com.example.data.repository

import com.example.domain.model.StudioFileItem

/**
 * Rejects stale or invalid editor selections before a project snapshot is persisted.
 */
object StudioProjectFileEditGuard {
    fun hasValidSelection(files: List<StudioFileItem>, selectedPath: String): Boolean {
        return selectedPath.isNotBlank() && files.any { it.path == selectedPath }
    }
}
