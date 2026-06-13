package domain

interface PoketUrlRepository {

    suspend fun save(mapping: PoketUrl)

    suspend fun findByCode(code: ShortCode): PoketUrl?
}
