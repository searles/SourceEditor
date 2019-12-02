package at.searles.sourceeditor

import android.text.Editable
import at.searles.parsingtools.formatter.EditableText

class EditTextAdapter(private val editable: Editable): EditableText, CharSequence by editable {
    override fun insert(position: Long, insertion: CharSequence) {
        editable.insert(position.toInt(), insertion)
    }

    override fun replace(start: Long, end: Long, replacement: CharSequence) {
        editable.replace(start.toInt(), end.toInt(), replacement)
    }
}