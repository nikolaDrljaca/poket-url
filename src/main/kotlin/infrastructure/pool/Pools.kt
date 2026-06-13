package infrastructure.pool

import domain.PoketUrl
import domain.ShortCode
import infrastructure.db.PoketUrlTable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.slf4j.Logger
import java.util.concurrent.atomic.AtomicInteger

class ShortCodePool(
    private val poolSize: Int = 10_000,
    private val logger: Logger
) {
    private val poolChannel = Channel<ShortCode>(capacity = poolSize)
    private val channelSize = AtomicInteger(0)

    private val populateMutex = Mutex()

    fun size(): Int = channelSize.get()

    suspend fun get(): ShortCode {
        val code = poolChannel.receive()
        channelSize.decrementAndGet()
        return code
    }

    // mutex lock prevets overlapping runs
    suspend fun populate() = populateMutex.withLock {
        val currentPoolSize = channelSize.get()
        logger.info("ShortCodePool - populating. Current size: $currentPoolSize.")
        // check if the pool needs more
        val needed = (poolSize - currentPoolSize).coerceAtLeast(0)
        if (needed == 0) {
            return@withLock
        }

        val codes = generateSequence { ShortCode.generate() }
            .distinct()
            .take(needed)
            .toList()
        val inDatabase = suspendTransaction {
            PoketUrlTable.select(PoketUrlTable.shortCode)
                .where { PoketUrlTable.shortCode inList codes.map { it.value } }
                .toSet()
                .map { it[PoketUrlTable.shortCode] }
        }
        val toStore = codes.filterNot { inDatabase.contains(it.value) }
        toStore.forEach { poolChannel.send(it) }
        channelSize.addAndGet(toStore.size)
        logger.info("ShortCodePool - added ${toStore.size} codes to pool.")
    }
}

