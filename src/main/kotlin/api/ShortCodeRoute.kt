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
import infrastructure.configuration.respondProblem
import io.ktor.http.*
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.slf4j.Logger


fun Route.shortCodeRoutes(
    logger: Logger,
    repository: PoketUrlRepository,
    shortCodeProvider: ShortCodeProvider,
    environment: Environment
) {
    val createUseCase = CreateShortUrlUseCase(
        repository = repository,
        shortCodeProvider = shortCodeProvider
    )
    val resolveUseCase = ResolveShortCodeUseCase(repository)

    rateLimit(Limiters.CreatePoketUrl) {
        createShortUrlRoute(
            createUseCase = createUseCase,
            logger = logger,
            basePath = environment.basePath
        )
    }

    rateLimit(Limiters.ResolveShortCode) {
        resolveShortCodeRoute(
            resolveUseCase = resolveUseCase
        )
    }
}

fun Route.resolveShortCodeRoute(
    resolveUseCase: ResolveShortCodeUseCase
) {
    get("/r/{code}") {
        val codeParam = call.parameters["code"] ?: return@get call.respondProblem(
            Problem(
                title = "Parameter {code} is required.",
                statusCode = HttpStatusCode.BadRequest,
                code = ErrorCode.EmptyShortCode
            )
        )
        val shortCode = ShortCode(codeParam).getOrElse { err ->
            val problem = Problem(
                title = "Parameter {code} is required.",
                statusCode = HttpStatusCode.BadRequest,
                code = ErrorCode.EmptyShortCode
            )
            call.respondProblem(problem)
            return@get
        }
        val result = resolveUseCase.execute(shortCode)
            ?: return@get call.respondProblem(
                problem = Problem(
                    title = "Original URL not found for code {${shortCode.value}}.",
                    statusCode = HttpStatusCode.NotFound,
                )
            )
        call.respondRedirect(result.value)
    }
}

fun Route.createShortUrlRoute(
    createUseCase: CreateShortUrlUseCase,
    basePath: String,
    logger: Logger
) {
    post("/shorten") {
        val request = call.receive<CreateShortCodeRequest>()
        val originalUrl = OriginalUrl(request.url).getOrElse { err ->
            val problem = when (err) {
                OriginalUrlError.ExceedLength -> Problem(
                    title = "URL lenght exceeded.",
                    statusCode = HttpStatusCode.BadRequest,
                    code = ErrorCode.UrlLengthExceeded
                )

                is OriginalUrlError.InvalidScheme -> Problem(
                    title = "Protocol ${err.scheme} is not supported.",
                    statusCode = HttpStatusCode.BadRequest,
                    code = ErrorCode.UnsupportedProtocol
                )

            }
            call.respondProblem(problem)
            return@post
        }

        createUseCase.execute(originalUrl)
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
                    val problem = Problem(
                        title = "Cannot create short code. Try again later.",
                        statusCode = HttpStatusCode.InternalServerError,
                        code = ErrorCode.CannotCreateShortCode
                    )
                    call.respondProblem(problem)
                }
            )
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
