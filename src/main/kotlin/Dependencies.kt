import application.CreateShortUrlUseCase
import application.ResolveShortCodeUseCase
import domain.DefaultShortCodeProvider
import infrastructure.cache.LruCache
import infrastructure.db.CachedPoketUrlRepository
import infrastructure.db.SqlitePoketUrlRepository
import infrastructure.pool.PoolShortCodeProvider
import io.ktor.server.application.Application
import io.ktor.server.application.log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.plus

data class Dependencies(
    val shortCodePool: PoolShortCodeProvider,
    val createShortCode: CreateShortUrlUseCase,
    val resolveShortCode: ResolveShortCodeUseCase
)

suspend fun Application.dependencies(config: PoketUrlConfiguration): Dependencies {
    val shortCodePool = PoolShortCodeProvider(
        coroutineScope = this + Dispatchers.IO,
        provider = DefaultShortCodeProvider()
    )
    // create dependencies
    val repo = CachedPoketUrlRepository(
        logger = log,
        delegate = SqlitePoketUrlRepository(logger = log),
        cache = LruCache()
    )
    // create use cases
    val createShortCode = CreateShortUrlUseCase(
        repository = repo,
        shortCodeProvider = shortCodePool
    )
    return Dependencies(
        shortCodePool = shortCodePool,
        createShortCode = createShortCode,
        resolveShortCode = ResolveShortCodeUseCase(repo)
    )
}

