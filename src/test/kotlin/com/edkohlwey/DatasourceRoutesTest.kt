package com.edkohlwey


import com.edkohlwey.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals

class DatasourceRoutesTest {

    @Test
    fun missingDatasourceNotFound() = testApplication {
        val client = setupClient()
        configureDatabaseAndRoutesForTest()

        client.get("/customers/test-customer/datasources/test-datasource").apply {
            assertEquals(HttpStatusCode.NotFound, status)
            assertEquals("", bodyAsText())
        }
    }

    @Test
    fun dataSourceCanBeAdded() = testApplication {
        val client = setupClient()
        configureDatabaseAndRoutesForTest()

        client.createTestCustomer()
        client.createTestDatasource()

        client.checkForTestDatasource()
    }

    @Test
    fun dataSourceCanBeUpdated() = testApplication {
        val client = setupClient()
        configureDatabaseAndRoutesForTest()

        client.createTestCustomer()
        client.createTestDatasource()
        client.checkForTestDatasource()
        client.put("/customers/test-customer/datasources/test-datasource") {
            contentType(ContentType.Application.Json)
            setBody(
                UpdateDatasourceRequest(
                    url = "test-url-2",
                    username = "test-username-2",
                    password = "test-password-2",
                    model = "test-model-2",
                    view = "test-view-2",
                    type = "LOOKER"
                )
            )
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("", bodyAsText())
        }

        client.checkForTestDatasource(
            expectedUrl = "test-url-2",
            expectedUsername = "test-username-2",
            expectedModel = "test-model-2",
            expectedType = "LOOKER",
            expectedView = "test-view-2"
        )
    }


    private fun ApplicationTestBuilder.configureDatabaseAndRoutesForTest() {
        application {
            configureSerialization()
            val database = configureDatabases()
            clearDatabases(database)
            configureCustomerRoutes(database)
            configureDatasourceRoutes(database)
        }
    }
}
