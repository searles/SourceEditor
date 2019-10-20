package at.searles.sourceeditor

import android.app.Activity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.EditText
import at.searles.storage.MapStorageFragment
import at.searles.storage.StorageFragment


class SyntaxEditorActivity : Activity() {
    /*
     * Editor
     */
    private lateinit var editor: EditText
    private lateinit var syntaxUpdater: DelayedUpdater
    private lateinit var storageFragment: StorageFragment<String>
    private var currentKey: String? = null // if null, there is no current key

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Init syntax highlighting
        initSyntaxUpdater()

        // init current key
        if(savedInstanceState != null) {
            currentKey = savedInstanceState.getString(CURRENT_KEY)
        }

        // init storage framework
        storageFragment =
                fragmentManager.findFragmentByTag(STORAGE_FRAGMENT_TAG) as StorageFragment<String>? ?:
                        MapStorageFragment().also {
                            fragmentManager.beginTransaction().add(storageFragment, STORAGE_FRAGMENT_TAG).commit()
                        }

        val saveButton: Button = findViewById(R.id.saveButton)

        saveButton.setOnClickListener {
            onSaveButtonClicked()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        if(currentKey != null) {
            outState.putString(CURRENT_KEY, currentKey)
        }
    }

    override fun onDestroy() {
        syntaxUpdater.cancel()
        super.onDestroy()
    }


    private fun onSaveButtonClicked() {
        val key = findViewById<EditText>(R.id.keyEditText).text.toString()

        if(currentKey != key && storageFragment.keyExists(key)) {
            // The dialog will call the save-method
            val ft = fragmentManager.beginTransaction()
            // Create and show the dialog.
            val dialogFragment = ReplaceExistingDialogFragment.newInstance(key)
            dialogFragment.show(ft, REPLACE_EXISTING_TAG)
        } else {
            // call save directly.
            save(key)
        }
    }

    fun save(key: String) {
        require(storageFragment.isValid(key)) { "invalid key! Please check this before" }
        val sourceCode = editor.text.toString()
        storageFragment.save(key, sourceCode)
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
        private val STORAGE_FRAGMENT_TAG = "sourceStorage"
        private val CURRENT_KEY = "currentKey"
        private val REPLACE_EXISTING_TAG = "replaceExisting"
    }
}
