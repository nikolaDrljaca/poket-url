package domain

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import java.net.URI

sealed interface OriginalUrlError {
    data object ExceedLength : OriginalUrlError
    data class InvalidScheme(val scheme: String) : OriginalUrlError
}

@JvmInline
value class OriginalUrl private constructor(val value: String) {
    companion object {
        const val MAX_LENGTH = 8192

        operator fun invoke(value: String): Either<OriginalUrlError, OriginalUrl> = either {
            val protocol = runCatching { URI.create(value).scheme }.getOrNull()
            ensureNotNull(protocol) {
                OriginalUrlError.InvalidScheme(value)
            }
            ensure(protocol in setOf("http", "https")) {
                OriginalUrlError.InvalidScheme(protocol)
            }
            ensure(value.length <= MAX_LENGTH) {
                OriginalUrlError.ExceedLength
            }
            OriginalUrl(value)
        }
    }
}
