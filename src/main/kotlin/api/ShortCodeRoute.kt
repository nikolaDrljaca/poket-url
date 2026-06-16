package api

import application.CreateShortUrlUseCase
import application.ResolveShortCodeUseCase
import arrow.core.getOrElse
import domain.OriginalUrl
import domain.OriginalUrlError
import domain.ShortCode
import domain.PoketUrlRepository
import domain.ShortCodeProvider
import infrastructure.configuration.Environment
import infrastructure.configuration.ErrorCode
import infrastructure.configuration.Limiters
import infrastructure.configuration.Problem
import infrastructure.configuration.respondErrorCode
import infrastructure.configuration.respondProblem
import io.ktor.http.*
import io.ktor.server.application.Application
import io.ktor.server.application.log
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.slf4j.Logger


fun Application.shortCodeRoutes(
    createShortCode: CreateShortUrlUseCase,
    resolveShortCode: ResolveShortCodeUseCase,
    environment: Environment
) {
    val logger = log
    val basePath = environment.basePath

    routing {
        get("/r/{code}") {
            val codeParam = call.parameters["code"] ?: return@get call.respondErrorCode(ErrorCode.EmptyShortCode)
            val shortCode = ShortCode(codeParam).getOrElse { err ->
                call.respondErrorCode(ErrorCode.EmptyShortCode)
                return@get
            }
            val result = resolveShortCode.execute(shortCode)
                ?: return@get call.respondProblem(
                    problem = Problem(
                        title = "Original URL not found for code {${shortCode.value}}.",
                        statusCode = HttpStatusCode.NotFound,
                    )
                )
            call.respondRedirect(result.value)
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
