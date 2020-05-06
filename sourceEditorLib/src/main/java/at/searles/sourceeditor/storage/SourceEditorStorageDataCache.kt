package at.searles.sourceeditor.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.text.format.DateFormat
import at.searles.android.storage.data.StorageDataCache
import at.searles.android.storage.data.StorageProvider
import at.searles.sourceeditor.R
import java.util.*

class SourceEditorStorageDataCache(private val context: Context, private val storageProvider: StorageProvider): StorageDataCache(storageProvider) {

    private val dateFormat = DateFormat.getDateFormat(context)

    override fun loadBitmap(name: String): Bitmap {
        return BitmapFactory.decodeResource(context.resources, R.drawable.ic_text)
    }

    override fun loadDescription(name: String): String {
        val file = storageProvider.findPathEntry(name) ?: return ""
        return "last modified: ${dateFormat.format(Date(file.lastModified()))}"
    }
}