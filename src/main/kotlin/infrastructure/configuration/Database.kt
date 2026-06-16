package infrastructure.configuration

import infrastructure.db.PoketUrlTable
import io.ktor.server.application.Application
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import java.sql.Connection

suspend fun Application.configureDatabase(
    environment: Environment
) {
    // extract env values
    val urlWithParams = buildString {
        append(environment.dbUrl)
        append("?journal_mode=WAL")
        append("&busy_timeout=5000")
        append("&synchronous=NORMAL")
        append("&foreign_keys=true")
        append("&cache_size=-32000")
        append("&mmap_size=268435456") // 256MB memory-mapped I/O, reduces syscall overhead
    }
    // single connection
    val db = Database.connect(
        url = urlWithParams,
        driver = "org.sqlite.JDBC"
    )
    // set sqlite compatible isolation level
    TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
    // initialize schemas, no migration tools like liquibase
    suspendTransaction(db) {
        SchemaUtils.create(PoketUrlTable)
    }
}