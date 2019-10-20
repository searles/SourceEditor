package at.searles.storage

import android.app.Fragment
import android.os.Bundle
import java.lang.RuntimeException
import java.util.stream.Stream

abstract class StorageFragment<A> : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    /**
     * Returns all keys to store elements in here.
     */
    abstract fun keys(): Stream<String>

    abstract fun keyExists(key: String): Boolean

    abstract fun save(key: String, value: A)

    abstract fun load(key: String): A

    abstract fun rename(oldKey: String, newKey: String)

    abstract fun delete(key: String)
    abstract fun isValid(key: A): Boolean

    class StorageExecption(cause: Exception): RuntimeException(cause)
}