package at.searles.sourceeditor

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import at.searles.android.storage.StorageActivity
import at.searles.android.storage.dialog.DiscardAndOpenDialogFragment
import at.searles.android.storage.dialog.ReplaceExistingDialogFragment
import at.searles.fractlang.CompilerInstance
import at.searles.fractlang.FractlangUtils
import at.searles.fractlang.extensions.FractlangObserver
import at.searles.fractlang.parsing.FractlangFormatter
import at.searles.fractlang.parsing.FractlangParser
import at.searles.fractlang.semanticanalysis.SemanticAnalysisException
import at.searles.parsing.ParserLookaheadException
import at.searles.parsing.ParserStream

class SyntaxEditorActivity : AppCompatActivity(), ReplaceExistingDialogFragment.Callback, DiscardAndOpenDialogFragment.Callback {

    private val sourceCodeEditor: EditText by lazy {
        findViewById<EditText>(R.id.sourceEditText)
    }

    private val fileNameEditor: EditText by lazy {
        findViewById<EditText>(R.id.nameEditText)
    }

    private val saveButton: Button by lazy {
        findViewById<Button>(R.id.saveButton)
    }

    private var sourceCode: String
        get() = sourceCodeEditor.text.toString()
        set(value) { sourceCodeEditor.setText(value)}

    private lateinit var syntaxUpdater: DelayedUpdater

    private var currentName: String? = null // if null, there is no current key
    private var isModified = false

    private lateinit var provider: SourceFilesProvider


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.source_editor_activity_main)

        // init current key
        if(savedInstanceState != null) {
            currentName = savedInstanceState.getString(currentNameKey)
            isModified = savedInstanceState.getBoolean(isModifiedKey)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString(currentNameKey, currentName)
        outState.putBoolean(isModifiedKey, isModified)
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

        val updateTask = SyntaxUpdateTask(
                sourceCodeEditor,
                textWatcher,
                FractlangObserver(sourceCodeEditor.resources, sourceCodeEditor.editableText),
                FractlangParser.program,
                FractlangParser.eof)

        syntaxUpdater = DelayedUpdater(updateTask, 500).apply { tick() }

        fileNameEditor.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                updateSaveButtonEnabled()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(resultCode == StorageActivity.openEntry) {
            val name = data!!.getStringExtra(StorageActivity.nameKey)!!
            if(!isModified) {
                discardAndOpen(name)
            } else {
                DiscardAndOpenDialogFragment.create(name).also {
                    supportFragmentManager.beginTransaction().add(it, "dialog").commit()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.source_editor_main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.open_storage_manager -> {
                startStorageActivity()
                true
            }
            R.id.compile -> {
                tryCompile()
                true
            }
            R.id.format -> {
                formatCode()
                false
            }
            R.id.return_program -> {
                false
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun discardAndOpen(name: String) {
        // this is also called if nothing has to be discarded.
        try {
            provider.load(name) { sourceCode = it }
        } catch(th: Throwable) {
            Toast.makeText(this, resources.getString(at.searles.android.storage.R.string.error, th.localizedMessage), Toast.LENGTH_LONG).show()
            return
        }

        fileNameEditor.setText(name)
        isModified = false
        currentName = name
        updateSaveButtonEnabled()
    }

    override fun replaceExistingAndSave(name: String) {
        try {
            provider.save(name, { sourceCode }, true)
        } catch(th: Throwable) {
            Toast.makeText(this, resources.getString(at.searles.android.storage.R.string.error, th.localizedMessage), Toast.LENGTH_LONG).show()
            return
        }

        isModified = false
        this.currentName = name
        updateSaveButtonEnabled()
    }

    private fun formatCode() {
        FractlangFormatter.format(EditTextAdapter(sourceCodeEditor.editableText))
    }

    private fun tryCompile() {
        try {
            CompilerInstance(ParserStream.fromString(sourceCode), FractlangUtils.instructions).analyze()
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

    fun onSaveButtonClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        val name = fileNameEditor.text.toString()
        val status: Boolean

        try {
            status = provider.save(name, {sourceCode}, name == currentName)
        } catch (th: Throwable) {
            Toast.makeText(this, resources.getString(at.searles.android.storage.R.string.error, th.localizedMessage), Toast.LENGTH_LONG).show()
            return
        }

        if(status) {
            isModified = false
            currentName = name
            updateSaveButtonEnabled()
        } else {
            ReplaceExistingDialogFragment.create(name)
                    .show(supportFragmentManager, "dialog")
        }
    }

    private fun updateSaveButtonEnabled() {
        val isEnabled = isModified || currentName != fileNameEditor.text.toString()
        saveButton.isEnabled = isEnabled
    }

    private fun startStorageActivity() {
        Intent(this, StorageActivity::class.java).also {
            // FIXME set title
            it.putExtra(StorageActivity.providerClassNameKey, provider.javaClass.canonicalName)
            startActivityForResult(it, storageActivityRequestCode)
        }
    }

    inner class ChangesTextWatcher: TextWatcher {
        override fun afterTextChanged(editable: Editable) {
            syntaxUpdater.tick()
            isModified = true
            updateSaveButtonEnabled()
        }
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    }

    companion object {
        private const val currentNameKey = "currentName"
        private const val isModifiedKey = "isModified"
        const val storageActivityRequestCode = 101
    }
}
