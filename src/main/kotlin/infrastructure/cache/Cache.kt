package infrastructure.cache

import domain.PoketUrl
import domain.ShortCode

interface Cache {
    suspend fun get(key: ShortCode): PoketUrl?

    suspend fun set(key: ShortCode, value: PoketUrl)
}

class InMemoryCache(
    private val cacheSize: Int = 10_000
) : Cache {
    // Poor mans LRU Cache
    private val capacity = cacheSize
    private val cache = object : LinkedHashMap<ShortCode, PoketUrl>(capacity, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<ShortCode, PoketUrl>) = size > capacity
    }

    override suspend fun get(key: ShortCode): PoketUrl? {
        return cache[key]
    }

    override suspend fun set(key: ShortCode, value: PoketUrl) {
        cache[key] = value
    }
}




