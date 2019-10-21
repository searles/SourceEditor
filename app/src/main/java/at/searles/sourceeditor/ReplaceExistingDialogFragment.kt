package at.searles.sourceeditor

import android.app.Dialog
import androidx.fragment.app.DialogFragment
import androidx.appcompat.app.AlertDialog
import android.os.Bundle

class ReplaceExistingDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(activity!!)
                .setTitle("Entry already exists")
                .setMessage("Do you want to replace the existing entry?")
                .setNegativeButton(android.R.string.no) { _, _ -> }
                .setPositiveButton(android.R.string.yes) { _, _ ->
                    // FIXME (activity as Saveable).save(arguments.getString(KEY_LABEL)!!)
                }
                .create()
    }

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
}