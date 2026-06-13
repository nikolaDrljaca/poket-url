import api.actuatorRoutes
import api.shortCodeRoutes
import infrastructure.configuration.configureHttp
import infrastructure.configuration.configureRateLimiter
import infrastructure.configuration.configureSerialization
import infrastructure.configuration.configureStatusPages
import infrastructure.configuration.parseEnvironment
import infrastructure.db.CachedPoketUrlRepository
import infrastructure.db.SqlitePoketUrlRepository
import infrastructure.db.configureDatabase
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.routing.*

suspend fun Application.module() {
    // parse environment
    val env = parseEnvironment()
    log.info("Loaded environment $env")

    // configure application plugins
    configureSerialization()
    configureStatusPages()
    configureHttp()
    configureDatabase(environment = env)
    configureRateLimiter(environment = env)
    // create dependencies
    val repo = CachedPoketUrlRepository(
        logger = log,
        delegate = SqlitePoketUrlRepository()
    )
    // configure routing
    routing {
        shortCodeRoutes(
            logger = log,
            repository = repo,
            environment = env
        )
        actuatorRoutes(logger = log)
    }
}

fun main(args: Array<String>) {
    EngineMain.main(args)
}
