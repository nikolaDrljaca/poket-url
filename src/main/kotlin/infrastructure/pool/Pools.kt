package infrastructure.pool

import domain.PoketUrl
import domain.ShortCode
import infrastructure.db.PoketUrlTable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.slf4j.Logger

class ShortCodePool(
    private val poolSize: Int = 9_000,
    private val logger: Logger
) {
    private val pool = ArrayDeque<ShortCode>()

    private val populateMutex = Mutex()

    fun get(): ShortCode {
        return pool.removeFirst()
    }

    // mutex lock prevets overlapping runs
    suspend fun populate() = populateMutex.withLock {
        logger.info("ShortCodePool - populating. Current size: ${pool.size}.")
        val codes = generateSequence { ShortCode.generate() }
            .take((poolSize - pool.size).coerceAtLeast(0))
            .toList()
        val inDatabase = suspendTransaction {
            PoketUrlTable.select(PoketUrlTable.shortCode)
                .where { PoketUrlTable.shortCode inList codes.map { it.value } }
                .toSet()
                .map { it[PoketUrlTable.shortCode] }
        }
        val toStore = codes.filterNot { inDatabase.contains(it.value) }
        pool.addAll(toStore)
        logger.info("ShortCodePool - added ${toStore.size} codes to pool.")
    }
}

// maybe you don't need this
class PoketUrlCreatePool(
    private val poolCapacity: Int = 1_000,
    private val logger: Logger
) {
    private val pool = ArrayDeque<PoketUrl>()
    private val mutex = Mutex()

    fun save(poketUrl: PoketUrl) {
        pool.addFirst(poketUrl)
    }

    suspend fun flush() = mutex.withLock {
        // wait for pool to fill
        if (pool.size < poolCapacity) return@withLock
        // pool is filled, flush
        suspendTransaction {
            PoketUrlTable.batchInsert(pool) {
                this[PoketUrlTable.shortCode] = it.shortCode.value
                this[PoketUrlTable.originalUrl] = it.originalUrl.value
            }
        }
        pool.clear()
    }
}
