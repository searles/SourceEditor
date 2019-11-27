package at.searles.sourceeditor

import at.searles.buf.Frame
import at.searles.parsing.ParserLookaheadException
import at.searles.parsing.ParserStream

interface SyntaxObserver {
    fun onToken(tokenId: Int, frame: Frame)
    fun onAnnotation(annotation: Any?, stream: ParserStream)
    fun onMissingEof(stream: ParserStream)
    fun onParserError(e: ParserLookaheadException)
}