package at.searles.fractlang.extensions

import android.content.res.Resources
import android.graphics.Typeface
import android.text.Editable
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import at.searles.buf.Frame
import at.searles.fractlang.parsing.Annot
import at.searles.fractlang.parsing.FractlangParser
import at.searles.parsing.ParserLookaheadException
import at.searles.parsing.ParserStream
import at.searles.sourceeditor.R
import at.searles.sourceeditor.SyntaxObserver
import kotlin.math.max
import kotlin.math.min

class FractlangObserver(private val resources: Resources, private val sourceCode: Editable): SyntaxObserver {
    override fun onToken(tokenId: Int, frame: Frame) {
        when (tokenId) {
            FractlangParser.mlComment.tokenId ->
                comment(frame.startPosition(), frame.endPosition())
            FractlangParser.slComment.tokenId ->
                comment(frame.startPosition(), frame.endPosition())
        }
    }

    override fun onAnnotation(annotation: Any?, stream: ParserStream) {
        if (stream.start == stream.end) {
            return
        }

        when (annotation) {
            Annot.Comma -> comma(stream.start, stream.end)
            Annot.DefKeyword -> declarationKeyword(stream.start, stream.end)
            Annot.Keyword -> keyword(stream.start, stream.end)
            Annot.Num -> number(stream.start, stream.end)
            Annot.Str -> string(stream.start, stream.end)
        }
    }

    override fun onMissingEof(stream: ParserStream) {
        val end = sourceCode.length
        error(stream.end, end.toLong())
    }

    override fun onParserError(e: ParserLookaheadException) {
        error(min((sourceCode.length - 1).toLong(), e.unexpectedTokenStart),
                max(sourceCode.length.toLong(), e.unexpectedTokenEnd))
    }

    fun error(start: Long, end: Long) {
        sourceCode.setSpan(BackgroundColorSpan(toColor(R.color.redBackgroundColor)), start.toInt(), end.toInt(), Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
    }

    private fun toColor(resourceId: Int): Int {
        return resources.getColor(resourceId, null)
    }

    override fun onUnexpectedEnd(stream: ParserStream) {
        val end = sourceCode.length
        error(max(0, stream.offset - 1), end.toLong())
    }

    private fun string(start: Long, end: Long) {
        sourceCode.setSpan(ForegroundColorSpan(toColor(R.color.blueTextColor)), start.toInt(), end.toInt(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    private fun number(start: Long, end: Long) {
        sourceCode.setSpan(ForegroundColorSpan(toColor(R.color.greenTextColor)), start.toInt(), end.toInt(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    private fun keyword(start: Long, end: Long) {
        sourceCode.setSpan(ForegroundColorSpan(toColor(R.color.violetTextColor)), start.toInt(), end.toInt(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        sourceCode.setSpan(StyleSpan(Typeface.BOLD), start.toInt(), end.toInt(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    private fun declarationKeyword(start: Long, end: Long) {
        sourceCode.setSpan(ForegroundColorSpan(toColor(R.color.orangeTextColor)), start.toInt(), end.toInt(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        sourceCode.setSpan(StyleSpan(Typeface.BOLD), start.toInt(), end.toInt(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    private fun comma(start: Long, end: Long) {
        sourceCode.setSpan(StyleSpan(Typeface.BOLD), start.toInt(), end.toInt(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    private fun comment(start: Long, end: Long) {
        sourceCode.setSpan(StyleSpan(Typeface.ITALIC), start.toInt(), end.toInt(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
}