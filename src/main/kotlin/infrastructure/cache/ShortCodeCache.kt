package infrastructure.cache

import domain.PoketUrl
import domain.ShortCode

interface ShortCodeCache {
    suspend operator fun get(key: ShortCode): PoketUrl?

    suspend operator fun set(key: ShortCode, value: PoketUrl)

    suspend fun getOrPut(key: ShortCode, loader: suspend () -> PoketUrl): PoketUrl

    companion object {
        const val SIZE = 10_000
    }
}

class LruCache(private val capacity: Int = ShortCodeCache.SIZE) : ShortCodeCache {
    private val cache = io.github.reactivecircus.cache4k.Cache.Builder<ShortCode, PoketUrl>()
        .maximumCacheSize(capacity.toLong())
        .build()

    override suspend fun get(key: ShortCode): PoketUrl? = cache.get(key)

    override suspend fun set(key: ShortCode, value: PoketUrl) = cache.put(key, value)

    override suspend fun getOrPut(key: ShortCode, loader: suspend () -> PoketUrl): PoketUrl {
        return cache.get(key, loader)
    }
}



