package domain

import java.security.SecureRandom

class DefaultShortCodeProvider : ShortCodeProvider {
    private val secureRandom = SecureRandom()
    private val untilCount = ShortCode.ALPHABET.count()

    override suspend fun get(): ShortCode {
        val generatedCode = buildString {
            repeat(ShortCode.LENGTH) {
                append(ShortCode.ALPHABET[secureRandom.nextInt(untilCount)])
            }
        }
        val shortCode = ShortCode(generatedCode).getOrNull()
        return requireNotNull(shortCode) { "ShortCodeProvider cannot generate an invalid ShortCode!" }
    }
}
