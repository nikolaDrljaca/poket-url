package api

import PoketUrlConfiguration
import application.CreateShortUrlUseCase
import application.ResolveShortCodeUseCase
import arrow.core.None
import arrow.core.Some
import arrow.core.getOrElse
import arrow.core.toOption
import domain.OriginalUrl
import domain.OriginalUrlError
import domain.ShortCode
import infrastructure.configuration.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.getValue
import kotlinx.serialization.Serializable


fun Application.shortCodeRoutes(
    createShortCode: CreateShortUrlUseCase,
    resolveShortCode: ResolveShortCodeUseCase,
    configuration: PoketUrlConfiguration
) {
    val logger = log
    val basePath = configuration.app.basePath

    routing {
        get("/r/{code}") {
            val code: String by call.parameters
            val shortCode = ShortCode(code).getOrElse { err ->
                log.error("Invalid ShortCode error: $err during resolution.")
                call.respondErrorCode(ErrorCode.InvalidShortCode)
                return@get
            }
            val originalUrl = resolveShortCode.execute(shortCode)
            when {
                originalUrl == null -> call.respondProblem(
                    problem = Problem(
                        title = "Original URL not found for code '${shortCode.value}'.",
                        statusCode = HttpStatusCode.NotFound,
                    )
                )

                else -> call.respondRedirect(originalUrl.value)
            }
        }

        post("/shorten") {
            val request = call.receive<CreateShortCodeRequest>()
            val originalUrl = OriginalUrl(request.url).getOrElse { err ->
                val errorCode = when (err) {
                    OriginalUrlError.ExceedLength -> ErrorCode.UrlLengthExceeded
                    is OriginalUrlError.InvalidScheme -> ErrorCode.UnsupportedProtocol
                }
                call.respondErrorCode(errorCode)
                return@post
            }

            createShortCode.execute(originalUrl)
                .map {
                    CreateShortCodeResponse(
                        key = it.value,
                        url = originalUrl.value,
                        fullUrl = "$basePath/r/${it.value}"
                    )
                }
                .fold(
                    onSuccess = { call.respond(HttpStatusCode.Created, it) },
                    onFailure = { err ->
                        logger.error("Error creating short url.", err)
                        call.respondErrorCode(ErrorCode.CannotCreateShortCode)
                    }
                )
        }
    }
}

@Serializable
data class CreateShortCodeRequest(val url: String)

@Serializable
data class CreateShortCodeResponse(
    // short code
    val key: String,
    // original url
    val url: String,
    val fullUrl: String
)
