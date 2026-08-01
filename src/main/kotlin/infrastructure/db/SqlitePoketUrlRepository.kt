package infrastructure.db

import arrow.core.raise.either
import domain.OriginalUrl
import domain.ShortCode
import domain.PoketUrl
import domain.PoketUrlRepository
import io.ktor.util.logging.Logger
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction

class SqlitePoketUrlRepository(
    private val logger: Logger
) : PoketUrlRepository {
    override suspend fun save(mapping: PoketUrl) {
        suspendTransaction {
            PoketUrlTable.insert {
                it[shortCode] = mapping.shortCode.value
                it[originalUrl] = mapping.originalUrl.value
            }
        }
    }

    override suspend fun findByCode(code: ShortCode): PoketUrl? = suspendTransaction {
        // retrieve from database
        val row = PoketUrlTable.selectAll()
            .where { PoketUrlTable.shortCode eq code.value }
            .singleOrNull()
            ?: return@suspendTransaction null
        // extract
        val originalUrl = row[PoketUrlTable.originalUrl]
        val storedCode = row[PoketUrlTable.shortCode]
        // map out
        val shortCode = either {
            PoketUrl(
                originalUrl = OriginalUrl(originalUrl).bind(),
                shortCode = ShortCode(storedCode).bind()
            )
        }
        shortCode.onLeft {
            logger.error("Could not parse ShortCode(${code.value}) stored in database due to: $it. SHOULD NOT HAPPEN!")
        }
        shortCode.getOrNull()
    }
}
