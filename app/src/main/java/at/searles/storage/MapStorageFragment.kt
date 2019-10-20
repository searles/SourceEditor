package at.searles.storage

import java.util.stream.Stream

class MapStorageFragment : StorageFragment<String>() {

    private val map = HashMap<String, String>()

    override fun keys(): Stream<String> = map.keys.stream()

    override fun keyExists(key: String): Boolean = map.contains(key)

    override fun save(key: String, value: String) {
        map.put(key, value)
    }

    override fun load(key: String): String = map.get(key)!!

    override fun rename(oldKey: String, newKey: String) {
        if(oldKey == newKey) return

        map[newKey] = map.remove(oldKey)!!
    }

    override fun delete(key: String) {
        map.remove(key)
    }

    override fun isValid(key: String): Boolean = true
}
