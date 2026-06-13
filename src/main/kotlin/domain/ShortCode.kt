package domain

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import java.security.SecureRandom

@JvmInline
value class ShortCode private constructor(val value: String) {
    companion object {
        private const val ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
        const val LENGTH = 8
        private val secureRandom = SecureRandom()

        fun generate(): ShortCode {
            val generatedCode = buildString {
                repeat(LENGTH) {
                    append(ALPHABET[secureRandom.nextInt(ALPHABET.count())])
                }
            }
            return ShortCode(generatedCode)
        }

        operator fun invoke(value: String): Either<PoketUrlError.EmptyShortCode, ShortCode> = either {
            ensure(value.isNotBlank()) { PoketUrlError.EmptyShortCode }
            ShortCode(value)
        }
    }
}
