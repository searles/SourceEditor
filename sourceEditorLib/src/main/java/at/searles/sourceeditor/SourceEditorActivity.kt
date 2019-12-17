package at.searles.sourceeditor

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import at.searles.android.storage.OpenSaveActivity
import at.searles.fractlang.CompilerInstance
import at.searles.fractlang.extensions.FractlangObserver
import at.searles.fractlang.parsing.FractlangFormatter
import at.searles.fractlang.parsing.FractlangParser
import at.searles.fractlang.semanticanalysis.SemanticAnalysisException
import at.searles.parsing.ParserLookaheadException

class SourceEditorActivity : OpenSaveActivity() {

    private val sourceCodeEditor: EditText by lazy {
        findViewById<EditText>(R.id.sourceEditText)
    }

    private var sourceCode: String
        get() = sourceCodeEditor.text.toString()
        set(value) { sourceCodeEditor.setText(value)}

    private lateinit var syntaxUpdater: DelayedUpdater


    override var contentString
        get() = sourceCode
        set(value) { sourceCode = value}

    override val fileNameEditor: EditText by lazy {
        findViewById<EditText>(R.id.fileNameEditText)
    }

    override lateinit var provider: SourceFilesProvider

    override val saveButton: Button by lazy {
        findViewById<Button>(R.id.saveButton)
    }

    override val storageActivityTitle: String
        get() = getString(R.string.openSourceFileTitle)

    override fun createReturnIntent(): Intent {
        return Intent().apply {
            putExtra(sourceKey, sourceCode)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.source_editor_activity_main)
    }

    override fun onDestroy() {
        syntaxUpdater.cancel()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()

        // since I don't need caching here, simply create it directly.
        provider = SourceFilesProvider(this)

        // Set up Syntax Highlighting
        val textWatcher = ChangesTextWatcher()

        val updateTask = SourceHighlightUpdateTask(
                sourceCodeEditor,
                textWatcher,
                FractlangObserver(sourceCodeEditor.resources, sourceCodeEditor.editableText),
                FractlangParser.program,
                FractlangParser.eof)

        syntaxUpdater = DelayedUpdater(updateTask, 500).apply { tick() }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.source_editor_main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.openStorageManager -> {
                startStorageActivity()
                true
            }
            R.id.compile -> {
                tryCompile()
                true
            }
            R.id.format -> {
                formatCode()
                true
            }
            R.id.returnProgram -> {
                finishAndReturnContent()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    private fun formatCode() {
        FractlangFormatter.format(EditTextAdapter(sourceCodeEditor.editableText))
    }

    private fun tryCompile() {
        try {
            CompilerInstance(sourceCode, emptyMap()).analyze()
        } catch(e: ParserLookaheadException) {
            sourceCodeEditor.setSelection(e.unexpectedTokenStart.toInt(), e.unexpectedTokenEnd.toInt())
            FractlangObserver(resources, sourceCodeEditor.editableText).onParserError(e)
            Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
        } catch(e: SemanticAnalysisException) {
            sourceCodeEditor.setSelection(e.trace.start.toInt(), e.trace.end.toInt())
            FractlangObserver(resources, sourceCodeEditor.editableText).error(e.trace.start, e.trace.end)
            Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
        }
    }

    inner class ChangesTextWatcher: TextWatcher {
        override fun afterTextChanged(editable: Editable) {
            syntaxUpdater.tick()
            contentChanged()
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    }

    companion object {
        const val sourceKey = "source"
    }
}
