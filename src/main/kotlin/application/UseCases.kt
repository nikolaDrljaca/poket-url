package application

import arrow.core.raise.result
import domain.OriginalUrl
import domain.ShortCode
import domain.PoketUrl
import domain.PoketUrlRepository
import infrastructure.pool.ShortCodePool


/*
Attempt up to 5 times to generate and insert a new ShortUrl using a ShortCode.
The `result` builder will catch any non-fatal exceptions, for which we don't care
what they are.
 */
class CreateShortUrlUseCase(
    private val repository: PoketUrlRepository,
    private val shortCodePool: ShortCodePool
) {
    suspend fun execute(original: OriginalUrl): Result<ShortCode> = result {
        val shortCode = shortCodePool.get()
        val code = PoketUrl(
            originalUrl = original,
            shortCode = shortCode
        )
        repository.save(code)
        code.shortCode
    }

    private suspend fun <T> retry(attempts: Int = 5, block: suspend () -> T): Result<T> {
        var result = result { block() }
        if (result.isSuccess) {
            return result
        }
        var hasFailed = result.isFailure
        var count = 0
        while (count < attempts && hasFailed) {
            count++
            result = result { block() }
            hasFailed = result.isFailure
        }
        return result
    }
}

class ResolveShortCodeUseCase(private val repository: PoketUrlRepository) {
    suspend fun execute(code: ShortCode): OriginalUrl? {
        return repository.findByCode(code)?.originalUrl
    }
}

