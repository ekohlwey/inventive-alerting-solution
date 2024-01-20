package com.edkohlwey


import com.edkohlwey.plugins.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.Test

class TriggerRoutesTest {

    @Test
    fun triggerCanBeAdded() = testApplication {
        val client = setupClient()
        configureDatabaseAndRoutesForTest()

        client.createTestCustomer()
        client.createTestCustomer(customerName = DEFAULT_RULE_DATASOURCE_CUSTOMER_NAME)
        client.createTestDatasource(customerName = DEFAULT_RULE_DATASOURCE_CUSTOMER_NAME)
        client.createTestRule()

        client.createTestEmailTrigger()

        client.checkForTestTrigger()
    }

    @Test
    fun missingTriggerNotFound() = testApplication {
        val client = setupClient()
        configureDatabaseAndRoutesForTest()

        client.checkForTestTrigger(expectedStatus = HttpStatusCode.NotFound)
    }

    private fun ApplicationTestBuilder.configureDatabaseAndRoutesForTest() {
        application {
            configureSerialization()
            val database = configureDatabases()
            clearDatabases(database)
            configureCustomerRoutes(database)
            configureDatasourceRoutes(database)
            configureRuleRoutes(database)
            configureTriggerRoutes(database)
        }
    }
}

