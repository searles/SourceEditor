package at.searles.sourceeditor.demo

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import at.searles.sourceeditor.SyntaxEditorActivity

class DemoActivity : AppCompatActivity() {

    private val runButton: Button by lazy {
        findViewById<Button>(R.id.button)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        runButton.setOnClickListener {
            Intent(this, SyntaxEditorActivity::class.java).also {
                startActivity(it)
            }
        }
    }
}
