package infrastructure.configuration

import io.ktor.server.application.*
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.ratelimit.RateLimitProviderConfig
import io.ktor.util.logging.*
import kotlin.time.Duration.Companion.seconds

object Limiters {
    val CreatePoketUrl = RateLimitName("create-poket-url")
    val ResolveShortCode = RateLimitName("resolve-short-code")
}

// Dynamically determine the tokens a request will use
// 0 means no tokens -> basically no rate limit is applied
private fun RateLimitProviderConfig.requestWeight(enabled: Boolean) = requestWeight { call, key ->
    when {
        enabled -> 1
        else -> 0
    }
}

fun Application.configureRateLimiter(
    environment: Environment
) {
    val logger = log

    if (environment.rateLimitEnabled) {
        logger.info("Installed Rate Limiter.")
    }

    install(RateLimit) {
        register(name = Limiters.CreatePoketUrl) {
            rateLimiter(limit = 10, refillPeriod = 30.seconds)
            requestWeight(environment.rateLimitEnabled)
        }

        register(name = Limiters.ResolveShortCode) {
            rateLimiter(limit = 100, refillPeriod = 10.seconds)
            requestWeight(environment.rateLimitEnabled)
        }
    }
}
