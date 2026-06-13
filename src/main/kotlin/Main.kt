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
import infrastructure.pool.ShortCodePool
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.routing.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

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
    val shortCodePool = ShortCodePool(logger = log)
    // interval based pool replenish
    launch {
        while (true) {
            shortCodePool.populate()
            delay(10.seconds)
        }
    }
    // emergency pool replenish
    launch {
        while (true) {
            if (shortCodePool.size() < 10_000 * 0.2) {
                log.warn("ShortCodePool - low watermark reached, triggering emergency populate.")
                shortCodePool.populate() // mutex inside prevents overlap with the regular loop
            }
            delay(1.seconds)
        }
    }

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
            shortCodePool = shortCodePool,
            environment = env
        )
        actuatorRoutes(logger = log)
    }
}

fun main(args: Array<String>) {
    EngineMain.main(args)
}
