package com.edkohlwey

import com.edkohlwey.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals

class RuleRoutesTest {

    @Test
    fun ruleCanBeAdded() = testApplication {
        val client = setupClient()
        configureDatabaseAndRoutesForTest()

        client.createTestCustomer()
        client.createTestCustomer(customerName = DEFAULT_RULE_DATASOURCE_CUSTOMER_NAME)
        client.createTestDatasource(customerName = DEFAULT_RULE_DATASOURCE_CUSTOMER_NAME)
        client.createTestRule()

        client.checkForTestRule()
    }

    @Test
    fun missingRuleNotFound() = testApplication {
        val client = setupClient()
        configureDatabaseAndRoutesForTest()

        client.get("/customers/test-customer/rules/unknown-rule").apply {
            assertEquals(HttpStatusCode.NotFound, status)
            assertEquals("", bodyAsText())
        }
    }


    private fun ApplicationTestBuilder.configureDatabaseAndRoutesForTest() {
        application {
            configureSerialization()
            val database = configureDatabases()
            clearDatabases(database)
            configureCustomerRoutes(database)
            configureDatasourceRoutes(database)
            configureRuleRoutes(database)
        }
    }
}
