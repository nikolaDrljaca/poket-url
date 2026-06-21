package domain

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure

sealed interface ShortCodeError {
    data object InvalidLength: ShortCodeError

    data object UnsupportedAlphabet: ShortCodeError
}

@JvmInline
value class ShortCode private constructor(val value: String) {
    companion object {
        const val ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
        const val LENGTH = 8

        private val allowedCharacters = ALPHABET.toSet()

        operator fun invoke(value: String): Either<ShortCodeError, ShortCode> = either {
            ensure(value.isNotBlank()) { ShortCodeError.InvalidLength }
            ensure(value.length == LENGTH) { ShortCodeError.InvalidLength }
            ensure(value.all { allowedCharacters.contains(it) }) {
                ShortCodeError.UnsupportedAlphabet
            }
            ShortCode(value)
        }
    }
}
