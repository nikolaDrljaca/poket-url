package infrastructure.db

import arrow.core.raise.result
import domain.ShortCode
import domain.PoketUrl
import domain.PoketUrlRepository
import infrastructure.cache.ShortCodeCache
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.slf4j.Logger

class CachedPoketUrlRepository(
    private val logger: Logger,
    private val delegate: PoketUrlRepository,
    private val cache: ShortCodeCache
) : PoketUrlRepository by delegate {

    override suspend fun save(mapping: PoketUrl) {
        delegate.save(mapping)
        cache[mapping.shortCode] = mapping
    }

    override suspend fun findByCode(code: ShortCode): PoketUrl? {
        cache[code]?.let { return it }
        logger.info("ShortCode ${code.value} cache MISS.")
        return delegate.findByCode(code)?.also { cache[code] = it }
    }
}
