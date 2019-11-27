package at.searles.sourceeditor

import android.content.res.Resources
import android.graphics.Typeface
import android.text.Editable
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import at.searles.buf.Frame
import at.searles.fractlang.parsing.Annot
import at.searles.fractlang.parsing.FractlangParser
import at.searles.parsing.ParserLookaheadException
import at.searles.parsing.ParserStream

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
        onParsingStoppedBeforeEof(stream.end)
    }

    override fun onParserError(e: ParserLookaheadException) {
        sourceCode.setSpan(ForegroundColorSpan(toColor(R.color.redTextColor)), e.beforeStart.toInt(),
                e.beforeEnd.toInt(), Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
        error(e.failedTokenStart, e.failedTokenEnd) // FIXME better name: unexpected token.
    }

    private fun toColor(resourceId: Int): Int {
        return resources.getColor(resourceId, null)
    }

    private fun onParsingStoppedBeforeEof(position: Long) {
        val end = sourceCode.length
        sourceCode.setSpan(StrikethroughSpan(), position.toInt(), end, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
    }

    private fun error(start: Long, end: Long) {
        sourceCode.setSpan(BackgroundColorSpan(0xffffaaaa.toInt()), start.toInt(), end.toInt(), Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
    }

    private fun string(start: Long, end: Long) {
        sourceCode.setSpan(ForegroundColorSpan(toColor(R.color.blueTextColor)), start.toInt(), end.toInt(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    private fun number(start: Long, end: Long) {
        sourceCode.setSpan(ForegroundColorSpan(toColor(R.color.blueTextColor)), start.toInt(), end.toInt(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    private fun keyword(start: Long, end: Long) {
        sourceCode.setSpan(ForegroundColorSpan(toColor(R.color.orangeTextColor)), start.toInt(), end.toInt(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        sourceCode.setSpan(StyleSpan(Typeface.BOLD), start.toInt(), end.toInt(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    private fun declarationKeyword(start: Long, end: Long) {
        sourceCode.setSpan(ForegroundColorSpan(toColor(R.color.magentaTextColor)), start.toInt(), end.toInt(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        sourceCode.setSpan(StyleSpan(Typeface.BOLD), start.toInt(), end.toInt(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    private fun comma(start: Long, end: Long) {
        sourceCode.setSpan(StyleSpan(Typeface.BOLD), start.toInt(), end.toInt(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    private fun comment(start: Long, end: Long) {
        sourceCode.setSpan(StyleSpan(Typeface.ITALIC), start.toInt(), end.toInt(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
}