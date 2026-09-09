package infrastructure.configuration

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.MissingRequestParameterException
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.uri
import io.ktor.server.response.*
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json

enum class ErrorCode(val code: String) {
    Unknown("PKT-001"),
    CannotCreateShortCode("PKT-002"),
    InvalidShortCode("PKT-006"),
    UnsupportedProtocol("PKT-004"),
    UrlLengthExceeded("PKT-005"),
}

fun ErrorCode.asProblem(): Problem = when (this) {
    ErrorCode.Unknown -> Problem(
        title = "Unknown exception.",
        statusCode = HttpStatusCode.InternalServerError,
        code = code
    )

    ErrorCode.CannotCreateShortCode -> Problem(
        title = "Cannot create short code. Try again later.",
        statusCode = HttpStatusCode.InternalServerError,
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

    ErrorCode.InvalidShortCode -> Problem(
        title = "Invalid short code.",
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
    val detail: String? = null,
    val status: Int = statusCode.value
)

suspend inline fun ApplicationCall.respondProblem(problem: Problem) =
    respondText(
        status = problem.statusCode,
        text = PoketUrlJson.encodeToString(problem.copy(instance = request.uri)),
        contentType = ContentType.Application.ProblemJson
    )

suspend inline fun ApplicationCall.respondErrorCode(err: ErrorCode) = respondProblem(err.asProblem())


fun Application.configureStatusPages() {
    val logger = log
    // all error responses use application/json+problem
    install(StatusPages) {
        exception<MissingRequestParameterException> { call, cause ->
            val problem = Problem(
                title = "Missing request parameter: ${cause.parameterName}.",
                statusCode = HttpStatusCode.BadRequest
            )
            call.respondProblem(problem)
        }

        exception<BadRequestException> { call, cause ->
            // Drill to access MissingFieldException
            val actual = cause.cause?.cause
            val title = when {
                actual is MissingFieldException -> "Missing fields: ${actual.missingFields.joinToString { it }}."
                else -> "Bad request."
            }
            val problem = Problem(
                title = title,
                statusCode = HttpStatusCode.BadRequest
            )
            call.respondProblem(problem)
        }

        exception<Throwable> { call, cause ->
            logger.error("Something went wrong!", cause)
            call.respondErrorCode(ErrorCode.Unknown)
        }
    }
}
