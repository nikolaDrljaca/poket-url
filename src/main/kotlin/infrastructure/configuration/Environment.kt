package infrastructure.configuration

import io.ktor.server.application.Application

data class Environment(
    val dbUrl: String,
    val basePath: String,
    val rateLimitEnabled: Boolean
) {
    override fun toString(): String {
        return buildString {
            appendLine("")
            appendLine("dbUrl: $dbUrl")
            appendLine("basePath: $basePath")
            appendLine("rateLimit: $rateLimitEnabled")
        }
    }
}

fun Application.parseEnvironment(): Environment {
    val rateLimit = environment.config.property("app.rateLimit.enabled").getString()
    val dbUrl = environment.config.property("app.database.url").getString()
    val basePath = environment.config.property("app.basePath").getString()

    return Environment(
        dbUrl = dbUrl,
        basePath = basePath,
        rateLimitEnabled = rateLimit == "true"
    )
}
