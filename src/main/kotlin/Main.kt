import api.actuatorRoutes
import api.shortCodeRoutes
import application.CreateShortUrlUseCase
import application.ResolveShortCodeUseCase
import domain.DefaultShortCodeProvider
import infrastructure.cache.LruCache
import infrastructure.configuration.*
import infrastructure.db.CachedPoketUrlRepository
import infrastructure.db.SqlitePoketUrlRepository
import infrastructure.configuration.configureDatabase
import infrastructure.pool.PoolShortCodeProvider
import infrastructure.pool.launchReplenishLoop
import io.ktor.server.application.*
import io.ktor.server.cio.*

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
        delegate = SqlitePoketUrlRepository(),
        cache = LruCache()
    )

    // configure routing
    shortCodeRoutes(
        createShortCode = CreateShortUrlUseCase(
            repository = repo,
            shortCodeProvider = shortCodePool
        ),
        resolveShortCode = ResolveShortCodeUseCase(repo),
        environment = env
    )
    actuatorRoutes()
}

fun main(args: Array<String>) {
    EngineMain.main(args)
}
