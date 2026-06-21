package domain

interface ShortCodeProvider {
    suspend fun get(): ShortCode
}

