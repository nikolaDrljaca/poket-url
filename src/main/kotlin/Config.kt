import kotlinx.serialization.Serializable

@Serializable
data class DatabaseConfiguration(
    val url: String
)

@Serializable
data class AppConfig(
    val basePath: String,
)

@Serializable
data class ServerConfiguration(
    val host: String,
    val port: Int
)

@Serializable
data class PoketUrlConfiguration(
    val server: ServerConfiguration,
    val app: AppConfig,
    val database: DatabaseConfiguration
)
