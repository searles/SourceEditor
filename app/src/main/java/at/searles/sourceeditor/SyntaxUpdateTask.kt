package at.searles.sourceeditor

import android.graphics.Color
import android.graphics.Typeface
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.*
import android.util.Log
import android.widget.EditText
import at.searles.meelan.parser.MeelanParser
import at.searles.parsing.ParserLookaheadException
import at.searles.parsing.ParserStream

class SyntaxUpdateTask(private val editor: EditText, private val textWatcher: TextWatcher) : Runnable {

    private val resources = editor.context.resources

    override fun run() {
        Log.d("SyntaxUpdateTask", "updating syntax")

        try {
            editor.removeTextChangedListener(textWatcher)

            clearHighlights()

            val inputStream = ParserStream.fromString(editor.editableText.toString())

            // Highlight comments.
            inputStream.tokStream().setListener { _, tokId, frame ->
                when (tokId) {
                    MeelanParser.multiLineComment() ->
                        comment(frame.startPosition().toInt(), frame.endPosition().toInt())
                    MeelanParser.singleLineComment() ->
                        comment(frame.startPosition().toInt(), frame.endPosition().toInt())
                }
            }

            inputStream.setListener(object : ParserStream.SimpleListener {
                override fun <C : Any?> annotate(src: ParserStream, annotation: C) {
                    if (src.start() == src.end()) {
                        return
                    }
                    when (annotation) {
                        MeelanParser.Annotation.KEYWORD_DEF -> definition(src.start().toInt(), src.end().toInt())
                        MeelanParser.Annotation.KEYWORD_PREFIX -> keyword(src.start().toInt(), src.end().toInt())
                        MeelanParser.Annotation.KEYWORD_INFIX -> keyword(src.start().toInt(), src.end().toInt())
                        MeelanParser.Annotation.STRING -> string(src.start().toInt(), src.end().toInt())
                        MeelanParser.Annotation.VALUE -> value(src.start().toInt(), src.end().toInt())
                        MeelanParser.Annotation.SEPARATOR -> comma(src.start().toInt(), src.end().toInt())
                    }
                }
            })

            try {
                MeelanParser.stmts().recognize(inputStream)

                if (!MeelanParser.eof().recognize(inputStream)) {
                    unparsed(inputStream.end().toInt())
                }
            } catch (e: ParserLookaheadException) {
                parserError(e.beforeStart.toInt(), e.beforeEnd.toInt(), e.failedTokenStart.toInt(), e.failedTokenEnd.toInt())
            }
        } finally {
            editor.addTextChangedListener(textWatcher)
        }
    }

    private fun color(resourceId: Int): Int {
        return resources.getColor(resourceId, null)
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

    private fun unparsed(start: Int) {
        val end = editor.editableText.length
        editor.editableText.setSpan(StrikethroughSpan(), start, end, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
    }

    private fun parserError(beforeStart: Int, beforeEnd: Int, tokenStart: Int, tokenEnd: Int) {
        editor.editableText.setSpan(BackgroundColorSpan(0xffffffaa.toInt()), beforeStart, beforeEnd, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
        error(tokenStart, tokenEnd)
    }

    private fun error(start: Int, end: Int) {
        editor.editableText.setSpan(BackgroundColorSpan(0xffffaaaa.toInt()), start, end, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
    }

    private fun string(start: Int, end: Int) {
        editor.editableText.setSpan(ForegroundColorSpan(color(R.color.blueTextColor)), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    private fun value(start: Int, end: Int) {
        editor.editableText.setSpan(ForegroundColorSpan(color(R.color.blueTextColor)), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    private fun keyword(start: Int, end: Int) {
        editor.editableText.setSpan(ForegroundColorSpan(color(R.color.orangeTextColor)), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        editor.editableText.setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    private fun definition(start: Int, end: Int) {
        editor.editableText.setSpan(ForegroundColorSpan(color(R.color.magentaTextColor)), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        editor.editableText.setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    private fun comma(start: Int, end: Int) {
        editor.editableText.setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    private fun comment(start: Int, end: Int) {
        editor.editableText.setSpan(StyleSpan(Typeface.ITALIC), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
}