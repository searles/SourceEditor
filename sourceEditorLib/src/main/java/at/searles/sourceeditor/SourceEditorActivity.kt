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
import at.searles.fractlang.FractlangProgram
import at.searles.fractlang.extensions.FractlangObserver
import at.searles.fractlang.parsing.FractlangFormatter
import at.searles.fractlang.parsing.FractlangParser
import at.searles.fractlang.semanticanalysis.SemanticAnalysisException
import at.searles.parsing.ParserLookaheadException

class SourceEditorActivity : OpenSaveActivity() {

    private val sourceCodeEditor: EditText by lazy {
        findViewById<EditText>(R.id.sourceEditText)
    }

    private lateinit var resetParametersMenuItem: MenuItem

    private val selectedParameters
        get() = if(!resetParametersMenuItem.isChecked) parameters else emptyMap()

    private var sourceCode: String
        get() = sourceCodeEditor.text.toString()
        set(value) { sourceCodeEditor.setText(value)}

    private lateinit var parameters: Map<String, String>

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
            putExtra(parametersKey, toBundle(parameters))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.source_editor_activity_main)

        if(savedInstanceState == null) {
            // fetch from intent
            sourceCode = intent.getStringExtra(sourceKey)!!
        }

        parameters = toStringMap(intent.getBundleExtra(parametersKey)!!)
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
                FractlangObserver(sourceCodeEditor.resources),
                FractlangParser.program,
                FractlangParser.eof)

        syntaxUpdater = DelayedUpdater(updateTask, 500).apply { tick() }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.source_editor_main_menu, menu)

        resetParametersMenuItem = menu.findItem(R.id.resetParameters)
        resetParametersMenuItem.isChecked = false

        if(parameters.isEmpty()) {
            resetParametersMenuItem.isEnabled = false
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.openStorageAction -> {
                startStorageActivity()
                true
            }
            R.id.format -> {
                formatCode()
                true
            }
            R.id.compile -> {
                tryCompile()
                true
            }
            R.id.returnAction -> {
                if(tryCompile()) {
                    finishAndReturnContent()
                }

                true
            }
            R.id.resetParameters -> {
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    private fun formatCode() {
        FractlangFormatter.format(EditTextAdapter(sourceCodeEditor.editableText))
    }

    private fun tryCompile(): Boolean {
        try {
            FractlangProgram(sourceCode, selectedParameters)
            Toast.makeText(this, getString(R.string.successfullyCompiled), Toast.LENGTH_SHORT).show()
            return true
        } catch(e: ParserLookaheadException) {
            sourceCodeEditor.setSelection(e.unexpectedTokenStart.toInt(), e.unexpectedTokenEnd.toInt())
            FractlangObserver(resources).onParserError(sourceCodeEditor.editableText, e)
            Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
            e.printStackTrace()
        } catch(e: SemanticAnalysisException) {
            sourceCodeEditor.setSelection(e.trace.start.toInt(), e.trace.end.toInt())
            FractlangObserver(resources).error(sourceCodeEditor.editableText, e.trace.start, e.trace.end)
            Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }

        return false
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
        const val parametersKey = "parameters"

        fun toBundle(map: Map<String, String>): Bundle {
            val bundle = Bundle()
            map.forEach { (key, value) -> bundle.putString(key, value) }
            return bundle
        }

        fun toStringMap(bundle: Bundle): Map<String, String> {
            return bundle.keySet().map { key -> key to bundle.getString(key)!! }.toMap()
        }
    }
}
