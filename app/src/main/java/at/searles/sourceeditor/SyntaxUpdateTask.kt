package at.searles.sourceeditor

import android.text.TextWatcher
import android.text.style.*
import android.widget.EditText
import at.searles.parsing.ParserLookaheadException
import at.searles.parsing.ParserStream
import at.searles.parsing.Recognizable

class SyntaxUpdateTask(private val editor: EditText,
                       private val updateTrigger: TextWatcher,
                       private val observer: SyntaxObserver,
                       private val parser: Recognizable,
                       private val eofParser: Recognizable) : Runnable {

    init {
        editor.addTextChangedListener(updateTrigger)
    }

    override fun run() {
        try {
            editor.removeTextChangedListener(updateTrigger)

            clearHighlights()

            val inputStream = ParserStream.fromString(editor.editableText.toString())

            // Highlight comments.
            inputStream.tokStream().setListener { _, tokId, frame ->
                observer.onToken(tokId, frame)
            }

            inputStream.setListener(object : ParserStream.SimpleListener {
                override fun <C : Any?> annotate(stream: ParserStream, annotation: C) {
                    observer.onAnnotation(annotation, stream)
                }
            })

            try {
                val status = parser.recognize(inputStream)

                if(!status) {
                    observer.onUnexpectedEnd(inputStream)
                } else if (!eofParser.recognize(inputStream)) {
                    observer.onMissingEof(inputStream)
                }
            } catch (e: ParserLookaheadException) {
                observer.onParserError(e)
            }
        } finally {
            editor.addTextChangedListener(updateTrigger)
        }
    }

    private fun clearHighlights() {
        editor.editableText.getSpans(0, editor.editableText.length, CharacterStyle::class.java).forEach {
            editor.editableText.removeSpan(it)
        }

        editor.editableText.getSpans(0, editor.editableText.length, StyleSpan::class.java).forEach {
            editor.editableText.removeSpan(it)
        }
    }
}