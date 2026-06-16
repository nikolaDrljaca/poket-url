package domain

import java.security.SecureRandom

interface ShortCodeProvider {
    suspend fun get(): ShortCode
}

class DefaultShortCodeProvider : ShortCodeProvider {
    private val secureRandom = SecureRandom()

    override suspend fun get(): ShortCode {
        val generatedCode = buildString {
            repeat(ShortCode.LENGTH) {
                append(ShortCode.ALPHABET[secureRandom.nextInt(ShortCode.ALPHABET.count())])
            }
        }
        val shortCode = ShortCode(generatedCode).getOrNull()
        return requireNotNull(shortCode) { "ShortCodeProvider cannot generate an invalid ShortCode!" }
    }
}

