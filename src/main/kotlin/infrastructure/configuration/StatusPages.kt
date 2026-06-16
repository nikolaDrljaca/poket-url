package infrastructure.configuration

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.uri
import io.ktor.server.response.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json

enum class ErrorCode(val code: String) {
    Unknown("PKT-001"),
    CannotCreateShortCode("PKT-002"),
    EmptyShortCode("PKT-003"),
    UnsupportedProtocol("PKT-004"),
    UrlLengthExceeded("PKT-005"),
}

fun ErrorCode.asProblem(): Problem = when (this) {
    ErrorCode.Unknown -> Problem(
        title = "Unknown exception",
        statusCode = HttpStatusCode.InternalServerError,
        code = code
    )

    ErrorCode.CannotCreateShortCode -> Problem(
        title = "Cannot create short code. Try again later.",
        statusCode = HttpStatusCode.InternalServerError,
        code = code
    )

    ErrorCode.EmptyShortCode -> Problem(
        title = "Parameter 'code' is required.",
        statusCode = HttpStatusCode.BadRequest,
        code = code
    )

    ErrorCode.UnsupportedProtocol -> Problem(
        title = "Protocol not supported.",
        statusCode = HttpStatusCode.BadRequest,
        code = code
    )

    ErrorCode.UrlLengthExceeded -> Problem(
        title = "URL length exceeded.",
        statusCode = HttpStatusCode.BadRequest,
        code = code
    )
}

// Content-Type application/problem+json
@Serializable
data class Problem(
    val type: String = "about:blank",
    val instance: String = "about:blank",
    val title: String, // exception message

    @Transient
    val statusCode: HttpStatusCode = HttpStatusCode.InternalServerError,

    val code: String? = null,
    val detail: String? = null
) {
    val status = statusCode.value
}


suspend inline fun ApplicationCall.respondProblem(problem: Problem) =
    respondText(
        status = problem.statusCode,
        text = Json.encodeToString(problem.copy(instance = request.uri)),
        contentType = ContentType.Application.ProblemJson
    )

suspend inline fun ApplicationCall.respondErrorCode(err: ErrorCode) = respondProblem(err.asProblem())


fun Application.configureStatusPages() {
    val logger = log
    // all error responses use application/json+problem
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            logger.error("Something went wrong!", cause)
            val problem = ErrorCode.Unknown.asProblem()
            call.respondProblem(problem)
        }
    }
}
