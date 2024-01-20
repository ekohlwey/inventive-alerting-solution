package com.edkohlwey

import com.edkohlwey.plugins.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    val credentials = loadDefaultCredentials()
    configureSerialization()
    val database = configureDatabases()
    configureCustomerRoutes(database)
    configureDatasourceRoutes(database)
    configureRuleRoutes(database)
    configureTriggerRoutes(database)
    jobSchedulerPlugin(database, credentials)
}
