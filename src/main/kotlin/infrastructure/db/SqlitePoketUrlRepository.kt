package infrastructure.db

import arrow.core.raise.either
import domain.OriginalUrl
import domain.ShortCode
import domain.PoketUrl
import domain.PoketUrlRepository
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction

class SqlitePoketUrlRepository : PoketUrlRepository {
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
        // map out
        val shortCode = either {
            PoketUrl(
                originalUrl = OriginalUrl(row[PoketUrlTable.originalUrl]).bind(),
                shortCode = ShortCode(row[PoketUrlTable.shortCode]).bind()
            )
        }
        shortCode.getOrNull()
    }
}
