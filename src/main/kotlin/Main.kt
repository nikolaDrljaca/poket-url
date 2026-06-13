import api.actuatorRoutes
import api.shortCodeRoutes
import domain.DefaultShortCodeProvider
import infrastructure.configuration.*
import infrastructure.db.CachedPoketUrlRepository
import infrastructure.db.SqlitePoketUrlRepository
import infrastructure.db.configureDatabase
import infrastructure.pool.PoolShortCodeProvider
import infrastructure.pool.launchReplenishLoop
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
    // initialize pool
    val shortCodePool = PoolShortCodeProvider(
        logger = log,
        provider = DefaultShortCodeProvider()
    )
    shortCodePool.launchReplenishLoop(this)

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
            shortCodeProvider = shortCodePool,
            environment = env
        )
        actuatorRoutes(logger = log)
    }
}

fun main(args: Array<String>) {
    EngineMain.main(args)
}
