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


class SyntaxEditorActivity : AppCompatActivity(), ReplaceExistingDialogFragment.Callback, DiscardAndOpenDialogFragment.Callback {

    private lateinit var editor: EditText
    private lateinit var syntaxUpdater: DelayedUpdater

    private var currentName: String? = null // if null, there is no current key
    private var isModified = false

    private lateinit var nameEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var provider: SourceFilesProvider


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // init current key
        if(savedInstanceState != null) {
            currentName = savedInstanceState.getString(currentNameKey)
            isModified = savedInstanceState.getBoolean(isModifiedKey)
        }

        editor = findViewById(R.id.sourceEditText)!!

        nameEditText = findViewById(R.id.nameEditText)
        saveButton = findViewById(R.id.saveButton)
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

        // === Syntax Highlighting ===
        val textWatcher = ChangesTextWatcher()
        val updateTask = SyntaxUpdateTask(editor, textWatcher)

        editor.addTextChangedListener(textWatcher)
        syntaxUpdater = DelayedUpdater(updateTask, 500)
        syntaxUpdater.tick() // schedule an update

        nameEditText.addTextChangedListener(object: TextWatcher {
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
        inflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.open_storage_manager -> {
                startStorageActivity()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private var content: String
        get() = editor.text.toString()
        set(value) { editor.setText(value)}

    override fun discardAndOpen(name: String) {
        // this is also called if nothing has to be discarded.
        try {
            provider.load(name) { content = it }
        } catch(th: Throwable) {
            Toast.makeText(this, resources.getString(at.searles.android.storage.R.string.error, th.localizedMessage), Toast.LENGTH_LONG).show()
            return
        }

        nameEditText.setText(name)
        isModified = false
        currentName = name
        updateSaveButtonEnabled()
    }

    override fun replaceExistingAndSave(name: String) {
        try {
            provider.save(name, { content }, true)
        } catch(th: Throwable) {
            Toast.makeText(this, resources.getString(at.searles.android.storage.R.string.error, th.localizedMessage), Toast.LENGTH_LONG).show()
            return
        }

        isModified = false
        this.currentName = name
        updateSaveButtonEnabled()
    }

    fun onSaveButtonClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        val name = nameEditText.text.toString()
        val status: Boolean

        try {
            status = provider.save(name, {content}, name == currentName)
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
        val isEnabled = isModified || currentName != nameEditText.text.toString()
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
