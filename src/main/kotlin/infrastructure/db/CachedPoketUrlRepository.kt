package infrastructure.db

import domain.ShortCode
import domain.PoketUrl
import domain.PoketUrlRepository
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.slf4j.Logger

/*
 * ShortCodePool
 * - hold at most CACHE_SIZE - 1000 codes
 * - run every X seconds, AND on start
 * - generate 1000 every run
 * To generate:
 * Batch generate 1000 codes
 * Check against the database
 * whichever ones are valid, store them in the pool
 *
 *
 * Batch Inserts
 * - Create use case should
 *   - Pull code off pool
 *   - store in cache when associated with url
 *   - store association in batch insert pool
 * - Batch insert pool stores at most 1000 and flushes to DB every Y seconds
 */

class CachedPoketUrlRepository(
    private val logger: Logger,
    private val delegate: PoketUrlRepository
) : PoketUrlRepository by delegate {
    // Poor mans LRU Cache
    private val capacity = 10_000
    private val cache = object : LinkedHashMap<String, PoketUrl>(capacity, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, PoketUrl>) = size > capacity
    }

    override suspend fun save(mapping: PoketUrl) = coroutineScope {
        launch {
            delegate.save(mapping)
        }
        cache[mapping.shortCode.value] = mapping
    }

    override suspend fun findByCode(code: ShortCode): PoketUrl? {
        return cache[code.value]
            ?: delegate.findByCode(code)?.also {
                logger.info("ShortCode ${code.value} cache MISS.")
                cache[code.value] = it
            }
    }
}
