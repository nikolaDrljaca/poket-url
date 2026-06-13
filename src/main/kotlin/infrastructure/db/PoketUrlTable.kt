package infrastructure.db

import domain.OriginalUrl
import domain.ShortCode
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime

object PoketUrlTable : Table(name = "poket_url") {
    val id = integer(name = "id").autoIncrement()

    val shortCode = varchar(name = "short_code", length = ShortCode.LENGTH + 1)
        .uniqueIndex(customIndexName = "idx_urls_shortcode")

    val originalUrl = varchar(name = "original_url", length = OriginalUrl.MAX_LENGTH)

    val createdAt = datetime(name = "created_at")
        .defaultExpression(CurrentDateTime)

    override val primaryKey: PrimaryKey = PrimaryKey(id)
}
