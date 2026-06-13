package domain

interface ShortCodeProvider {
    suspend fun get(): ShortCode
}

class DefaultShortCodeProvider : ShortCodeProvider {
    override suspend fun get(): ShortCode {
        return ShortCode.generate()
    }
}

