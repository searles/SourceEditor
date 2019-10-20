package at.searles.sourceeditor

import android.app.AlertDialog
import android.app.Dialog
import android.app.DialogFragment
import android.os.Bundle
import at.searles.storage.Saveable

class ReplaceExistingDialogFragment : DialogFragment() {

    companion object {
        val KEY_LABEL = "key"

        fun newInstance(key: String): ReplaceExistingDialogFragment {
            return ReplaceExistingDialogFragment().also {
                val args = Bundle()
                args.putString(KEY_LABEL, key)
                it.arguments = args
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle): Dialog {
        return AlertDialog.Builder(activity)
                .setTitle("Entry already exists")
                .setMessage("Do you want to replace the existing entry?")
                .setNegativeButton(android.R.string.no) { _, _ -> }
                .setPositiveButton(android.R.string.yes) { _, _ ->
                    (activity as Saveable).save(arguments.getString(KEY_LABEL)!!)
                }
                .create()
    }
}