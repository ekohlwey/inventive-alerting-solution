package com.edkohlwey

import com.edkohlwey.plugins.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import io.ktor.util.reflect.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.assertEquals


const val DEFAULT_CUSTOMER_NAME = "test-customer"
const val DEFAULT_DATASOURCE_NAME = "test-datasource"
const val DEFAULT_DATASOURCE_URL = "http://test-url"
const val DEFAULT_DATASOURCE_USERNAME = "test-username"
const val DEFAULT_DATASOURCE_PASSWORD = "test-password"
suspend fun HttpClient.createTestCustomer(
    customerName: String = DEFAULT_CUSTOMER_NAME,
    expectedStatus: HttpStatusCode = HttpStatusCode.Created
) {
    this.post("/customers") {
        contentType(ContentType.Application.Json)
        setBody(CreateCustomerRequest(name = customerName))
    }.apply {
        assertEquals(expectedStatus, status)
        assertEquals("", bodyAsText())
    }
}

suspend fun HttpClient.createTestDatasource(
    customerName: String = DEFAULT_CUSTOMER_NAME,
    datasourceName: String = DEFAULT_DATASOURCE_NAME,
    datasourceUrl: String = DEFAULT_DATASOURCE_URL,
    datasourceUsername: String = DEFAULT_DATASOURCE_USERNAME,
    datasourcePassword: String = DEFAULT_DATASOURCE_PASSWORD,
    expectedStatus: HttpStatusCode = HttpStatusCode.Created
) {
    this.post("/customers/${customerName}/datasources") {
        contentType(ContentType.Application.Json)
        setBody(
            CreateDatasourceRequest(
                name = datasourceName,
                url = datasourceUrl,
                username = datasourceUsername,
                password = datasourcePassword
            )
        )
    }.apply {
        assertEquals(expectedStatus, status)
        assertEquals("", bodyAsText())
    }
}

suspend fun HttpClient.checkForTestDatasource(
    customerName: String = DEFAULT_CUSTOMER_NAME,
    datasourceName: String = DEFAULT_DATASOURCE_NAME,
    expectedUrl: String = DEFAULT_DATASOURCE_URL,
    expectedUsername: String = DEFAULT_DATASOURCE_USERNAME
) {
    this.get("/customers/${customerName}/datasources/${datasourceName}").apply {
        assertEquals(HttpStatusCode.OK, status)
        assertEquals(
            GetDatasourceResponse(
                name = datasourceName,
                url = expectedUrl,
                username = expectedUsername
            ), body(typeInfo<GetDatasourceResponse>())
        )
    }
}

suspend fun HttpClient.checkForTestCustomer(
    customerName: String = DEFAULT_CUSTOMER_NAME,
    expectedStatus: HttpStatusCode = HttpStatusCode.OK
) {
    this.get("/customers/${customerName}").apply {
        assertEquals(expectedStatus, status)
        if (expectedStatus == HttpStatusCode.OK) {
            assertEquals(GetCustomerResponse(name = customerName), body(typeInfo<GetCustomerResponse>()))
        } else {
            assertEquals("", bodyAsText())
        }
    }
}

fun ApplicationTestBuilder.setupClient(): HttpClient {
    val client = createClient {
        install(ContentNegotiation) {
            json()
        }
    }
    return client
}

fun clearDatabases(database: Database) {
    transaction(database) {
        DataSources.deleteAll()
        Customers.deleteAll()
    }
}