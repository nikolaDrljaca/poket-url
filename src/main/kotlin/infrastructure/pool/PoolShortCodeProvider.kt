package infrastructure.pool

import domain.ShortCode
import domain.ShortCodeProvider
import arrow.core.raise.result
import infrastructure.db.PoketUrlTable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.slf4j.Logger
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.seconds

class PoolShortCodeProvider(
    private val poolSize: Int = POOL_SIZE,
    private val logger: Logger,
    private val provider: ShortCodeProvider
) : ShortCodeProvider by provider {

    private val pool = Channel<ShortCode>(capacity = poolSize)
    private val currentPoolSize = AtomicInteger(0)

    private val populateMutex = Mutex()

    fun watermark(): Boolean = currentPoolSize.get() < 0.2 * poolSize

    override suspend fun get(): ShortCode {
        val code = pool.receive()
        currentPoolSize.decrementAndGet()
        return code
    }

    // mutex lock prevets overlapping runs
    // result wrapper swallows exceptions other than fatal ones
    suspend fun populate() = result {
        populateMutex.withLock {
            val current = currentPoolSize.get()
            // check if the pool needs more
            val needed = (poolSize - current).coerceAtLeast(0)
            if (needed == 0) {
                return@withLock
            }
            // generate and store new codes
            val codes = buildSet {
                while (size < needed) add(provider.get())
            }
            val inDatabase = suspendTransaction {
                PoketUrlTable.select(PoketUrlTable.shortCode)
                    .where { PoketUrlTable.shortCode inList codes.map { it.value } }
                    .toSet()
                    .map { it[PoketUrlTable.shortCode] }
            }
            val toStore = codes.filterNot { inDatabase.contains(it.value) }
            toStore.forEach { pool.send(it) }
            currentPoolSize.addAndGet(toStore.size)
        }
    }

    companion object {
        const val POOL_SIZE = 10_000
    }
}

fun PoolShortCodeProvider.launchReplenishLoop(scope: CoroutineScope) {
    // interval based pool replenish
    scope.launch {
        while (true) {
            populate()
            delay(30.seconds)
        }
    }
    // emergency pool replenish
    // if pool is below watermark
    scope.launch {
        while (true) {
            if (watermark()) {
                populate()
            }
            delay(1.seconds)
        }
    }
}


