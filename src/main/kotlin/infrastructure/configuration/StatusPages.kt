package infrastructure.configuration

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json

@JvmInline
@Serializable
value class ErrorCode private constructor(val code: String) {
    companion object {
        const val APP_PREFIX = "PKT"

        val Unknown = ErrorCode("$APP_PREFIX-001")
        val CannotCreateShortCode = ErrorCode("$APP_PREFIX-002")
        val EmptyShortCode = ErrorCode("$APP_PREFIX-003")
        val UnsupportedProtocol = ErrorCode("$APP_PREFIX-004")
        val UrlLengthExceeded = ErrorCode("$APP_PREFIX-005")
    }
}

// Content-Type application/problem+json
@Serializable
data class Problem(
    val type: String = "about:blank",
    val instance: String = "about:blank",
    val title: String, // exception message

    @Transient
    val statusCode: HttpStatusCode = HttpStatusCode.InternalServerError,

    val code: ErrorCode? = null,
    val details: String? = null
) {
    val status = statusCode.value
}


suspend fun ApplicationCall.respondProblem(problem: Problem) =
    respondText(
        status = problem.statusCode,
        text = Json.encodeToString(problem),
        contentType = ContentType.Application.ProblemJson
    )

// RuntimeExceptions should be raise alerts!
// Move all configurations to the infrastructure.ktor package
fun Application.configureStatusPages() {
    // all error responses use application/json+problem
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            val problem = Problem(
                title = cause.localizedMessage,
                statusCode = HttpStatusCode.InternalServerError,
                code = ErrorCode.Unknown
            )
            call.respondText(
                contentType = ContentType.Application.ProblemJson,
                status = HttpStatusCode.InternalServerError,
                text = Json.encodeToString(problem)
            )
        }
    }
}
