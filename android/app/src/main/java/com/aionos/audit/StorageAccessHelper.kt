package com.aionos.audit

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity

/**
 * Helper class for Storage Access Framework operations.
 * Provides a clean API for document creation and export using SAF.
 */
class StorageAccessHelper(private val activity: FragmentActivity) {
    
    companion object {
        const val MIME_TYPE_CSV = "text/csv"
        const val DEFAULT_FILENAME = "aionos-audit-export.csv"
    }
    
    /**
     * Creates an intent to save a CSV file using Storage Access Framework.
     * The user will be presented with a file picker to choose where to save.
     * 
     * @param suggestedFilename The suggested filename (without extension)
     * @return Intent ready to be launched with ActivityResultContracts
     */
    fun createSaveCsvIntent(suggestedFilename: String = DEFAULT_FILENAME): Intent {
        return Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            type = MIME_TYPE_CSV
            putExtra(Intent.EXTRA_TITLE, "$suggestedFilename.csv")
            // Optionally add initial directory
            // putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri)
            addCategory(Intent.CATEGORY_OPENABLE)
        }
    }
    
    /**
     * ActivityResultContract for creating a document.
     * Use with rememberLauncherForActivityResult in Compose.
     */
    val createDocument = ActivityResultContracts.CreateDocument(MIME_TYPE_CSV)
    
    /**
     * Checks if Storage Access Framework is available on this device.
     */
    fun isSafAvailable(): Boolean {
        val intent = createSaveCsvIntent()
        return activity.packageManager.queryIntentActivities(intent, 0).isNotEmpty()
    }
    
    /**
     * Gets the display name for a URI (filename).
     */
    fun getDisplayName(uri: Uri): String? {
        return uri.lastPathSegment?.takeIf { it.isNotBlank() }
    }
}
