import api.actuatorRoutes
import api.shortCodeRoutes
import infrastructure.configuration.configureDatabase
import infrastructure.configuration.configureHttp
import infrastructure.configuration.configureSerialization
import infrastructure.configuration.configureStatusPages
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.config.*
import io.ktor.server.engine.*

fun main() {
    // extract configuration
    val config = ApplicationConfig("application.yaml")
        .getAs<PoketUrlConfiguration>()
    // configure embedded server
    embeddedServer(
        factory = CIO,
        host = config.server.host,
        port = config.server.port,
        module = {
            val deps = dependencies(config)
            poketUrlServiceApp(
                configuration = config,
                dependencies = deps
            )
        }
    ).start(wait = true)
}

suspend fun Application.poketUrlServiceApp(
    configuration: PoketUrlConfiguration,
    dependencies: Dependencies
) {
    // configure application plugins
    configureSerialization()
    configureHttp()
    configureStatusPages()
    configureDatabase(databaseConfiguration =  configuration.database)
    // start pool replenish loop
    dependencies.shortCodePool.launchReplenishLoop()

    // routes
    shortCodeRoutes(
        createShortCode = dependencies.createShortCode,
        resolveShortCode = dependencies.resolveShortCode,
        configuration = configuration
    )
    actuatorRoutes()
}
