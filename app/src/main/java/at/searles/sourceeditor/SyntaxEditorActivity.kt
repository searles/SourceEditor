package at.searles.sourceeditor

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import at.searles.android.storage.StorageActivity


class SyntaxEditorActivity : Activity() {
    /*
     * Editor
     */
    private lateinit var editor: EditText
    private lateinit var syntaxUpdater: DelayedUpdater
    private var currentName: String? = null // if null, there is no current key

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Init syntax highlighting
        initSyntaxUpdater()

        // init current key
        if(savedInstanceState != null) {
            currentName = savedInstanceState.getString(currentName)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.open_storage_manager -> {
                Intent(this, StorageActivity::class.java).also {
                    // FIXME!
                    it.putExtra(StorageActivity.providerClassNameKey, DemoProvider::class.java)
                    startActivityForResult(it, openNameId)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        if(currentName != null) {
            outState.putString(currentName, currentName)
        }
    }

    override fun onDestroy() {
        syntaxUpdater.cancel()
        super.onDestroy()
    }

    fun onOpenButtonClicked(view: View) {
        Intent(this, StorageActivity::class.java).also {
            // FIXME!
            it.putExtra(StorageActivity.providerClassNameKey, DemoProvider::class.java.canonicalName)
            startActivityForResult(it, openNameId)
        }
    }

    fun onSaveButtonClicked(view: View) {
        val name = findViewById<EditText>(R.id.nameEditText).text.toString()

        /* FIXME if(currentName != key && storageFragment.keyExists(key)) {
            // The dialog will call the save-method
            val ft = fragmentManager.beginTransaction()
            // Create and show the dialog.
            val dialogFragment = ReplaceExistingDialogFragment.newInstance(key)
            dialogFragment.show(ft, REPLACE_EXISTING_TAG)
        } else {
            // call save directly.
            save(key)
        }*/
    }

    fun save(name: String) {
        // FIXME
        val sourceCode = editor.text.toString()
    }

    private fun initSyntaxUpdater() {
        editor = findViewById(R.id.sourceEditText)!!

        val textWatcher = ChangesTextWatcher()
        val updateTask = SyntaxUpdateTask(editor, textWatcher)

        editor.addTextChangedListener(textWatcher)
        syntaxUpdater = DelayedUpdater(updateTask, 500)
        syntaxUpdater.tick() // schedule an update
    }

    inner class ChangesTextWatcher: TextWatcher {
        override fun afterTextChanged(editable: Editable) {
            syntaxUpdater.tick()
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            Log.d("SyntaxEditorActivity", "beforeTextChanged")
        }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            Log.d("SyntaxEditorActivity", "onTextChanged")
        }
    }

    companion object {
        private const val currentName = "currentName"
        private const val openNameId = 146
        private val REPLACE_EXISTING_TAG = "replaceExisting"
    }
}
