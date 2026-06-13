package api

import arrow.core.raise.result
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.util.logging.Logger
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction

fun Route.actuatorRoutes(
    logger: Logger
) {
    get("/health") {
        val test = result {
            suspendTransaction {
                exec("SELECT 1;")
            }
        }
        val httpStatusCode = test.fold(
            onSuccess = { HttpStatusCode.OK },
            onFailure = { HttpStatusCode.ServiceUnavailable }
        )
        call.respond(status = httpStatusCode, message = "")
    }
}