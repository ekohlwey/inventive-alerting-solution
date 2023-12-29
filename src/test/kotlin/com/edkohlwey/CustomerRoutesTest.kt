package com.edkohlwey

import com.edkohlwey.plugins.configureCustomerRoutes
import com.edkohlwey.plugins.configureDatabases
import com.edkohlwey.plugins.configureSerialization
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.Test

class CustomerRoutesTest {
    @Test
    fun missingCustomerReturnsNotFound() = testApplication {
        val client = setupClient()
        setupDatabaseAndRoutes()

        client.checkForTestCustomer(expectedStatus = HttpStatusCode.NotFound)
    }

    @Test
    fun customerCanBeAdded() = testApplication {
        val client = setupClient()
        setupDatabaseAndRoutes()

        client.createTestCustomer()

        client.checkForTestCustomer()
    }
}

private fun ApplicationTestBuilder.setupDatabaseAndRoutes() {
    application {
        configureSerialization()
        val database = configureDatabases()
        clearDatabases(database)
        configureCustomerRoutes(database)
    }
}
