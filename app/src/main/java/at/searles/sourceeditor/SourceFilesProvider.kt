package at.searles.sourceeditor

import android.content.Context
import android.widget.ImageView
import at.searles.android.storage.data.FilesProvider
import com.bumptech.glide.Glide

class SourceFilesProvider(context: Context): FilesProvider(context.getDir(directoryName, 0)) {
    override fun setImageInView(name: String, imageView: ImageView) {
        Glide
                .with(imageView.context)
                .load(R.drawable.ic_launcher_foreground)
                .centerCrop()
                // TODO .placeholder(R.drawable.loading_spinner)
                .into(imageView)
    }

    companion object {
        private const val directoryName = "demo"
    }
}