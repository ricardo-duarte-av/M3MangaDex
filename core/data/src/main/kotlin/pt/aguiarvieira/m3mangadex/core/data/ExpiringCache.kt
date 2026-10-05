package pt.aguiarvieira.m3mangadex.core.data

/**
 * A small LRU of values that go stale after [ttlMillis]. Opening the reader right after the
 * details screen (or going back) shouldn't refetch a 1000-chapter feed, but a value shouldn't
 * live long enough to hide new chapters either.
 */
internal class ExpiringCache<K : Any, V : Any>(
    private val maxSize: Int,
    private val ttlMillis: Long,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    private class Entry<V>(
        val value: V,
        val storedAt: Long,
    )

    private val entries = LinkedHashMap<K, Entry<V>>(maxSize, LOAD_FACTOR, true)

    @Synchronized
    operator fun get(key: K): V? {
        val entry = entries[key] ?: return null
        if (nowMillis() - entry.storedAt > ttlMillis) {
            entries.remove(key)
            return null
        }
        return entry.value
    }

    @Synchronized
    operator fun set(
        key: K,
        value: V,
    ) {
        entries[key] = Entry(value, nowMillis())
        while (entries.size > maxSize) entries.remove(entries.keys.first())
    }

    suspend fun getOrPut(
        key: K,
        load: suspend () -> V,
    ): V = get(key) ?: load().also { set(key, it) }

    private companion object {
        const val LOAD_FACTOR = 0.75f
    }
}
