package application

import arrow.core.raise.result
import domain.OriginalUrl
import domain.ShortCode
import domain.PoketUrl
import domain.PoketUrlRepository
import domain.ShortCodeProvider


class CreateShortUrlUseCase(
    private val repository: PoketUrlRepository,
    private val shortCodeProvider: ShortCodeProvider
) {
    suspend fun execute(original: OriginalUrl): Result<ShortCode> = result {
        val shortCode = shortCodeProvider.get()
        val code = PoketUrl(
            originalUrl = original,
            shortCode = shortCode
        )
        repository.save(code)
        code.shortCode
    }
}

class ResolveShortCodeUseCase(private val repository: PoketUrlRepository) {
    suspend fun execute(code: ShortCode): OriginalUrl? {
        return repository.findByCode(code)?.originalUrl
    }
}

