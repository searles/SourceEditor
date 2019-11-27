package at.searles.sourceeditor

import android.text.TextWatcher
import android.text.style.*
import android.util.Log
import android.widget.EditText
import at.searles.parsing.ParserLookaheadException
import at.searles.parsing.ParserStream
import at.searles.parsing.Recognizable

class SyntaxUpdateTask(private val editor: EditText, private val textWatcher: TextWatcher) : Runnable {

    private val observer: SyntaxObserver = FractlangObserver(editor.resources, editor.editableText)
    private lateinit var parser: Recognizable

    override fun run() {
        try {
            editor.removeTextChangedListener(textWatcher)

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
                parser.recognize(inputStream)

                // TODO if (!FractlangParser.eof().recognize(inputStream)) {
                    //observer.onMissingEof(inputStream)
                //}
            } catch (e: ParserLookaheadException) {
                observer.onParserError(e)
            }
        } finally {
            editor.addTextChangedListener(textWatcher)
        }
    }

    private fun clearHighlights() {
        Log.d("SyntaxUpdateTask", "removing all spans")

        editor.editableText.getSpans(0, editor.editableText.length, CharacterStyle::class.java).forEach {
            editor.editableText.removeSpan(it)
        }

        editor.editableText.getSpans(0, editor.editableText.length, StyleSpan::class.java).forEach {
            editor.editableText.removeSpan(it)
        }
    }
}