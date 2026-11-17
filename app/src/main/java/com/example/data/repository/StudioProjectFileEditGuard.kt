package com.example.data.repository

import com.example.domain.model.StudioFileItem

/**
 * Rejects stale or invalid editor selections before a project snapshot is persisted.
 */
object StudioProjectFileEditGuard {
    fun hasValidSelection(files: List<StudioFileItem>, selectedPath: String): Boolean {
        return selectedPath.isNotBlank() && files.any { it.path == selectedPath }
    }

    /**
     * Returns a copied file list with only the selected file content changed.
     * A null result means the editor selection is stale or invalid.
     */
    fun updateSelectedFileContent(
        files: List<StudioFileItem>,
        selectedPath: String,
        newContent: String
    ): List<StudioFileItem>? {
        if (!hasValidSelection(files, selectedPath)) return null
        return files.map { file ->
            if (file.path == selectedPath) file.copy(content = newContent) else file
        }
    }
}
