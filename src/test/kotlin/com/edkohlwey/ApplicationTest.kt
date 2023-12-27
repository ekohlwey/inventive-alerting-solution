package com.edkohlwey

import com.edkohlwey.plugins.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.server.testing.*
import io.ktor.util.reflect.*
import kotlin.test.*

class ApplicationTest {
    @Test
    fun missingCustomerReturnsNotFound() = testApplication {
        val client = createClient {
            install(ContentNegotiation){
                json()
            }
        }
        application {
            configureDatabases()
            configureSerialization()
        }
        client.get("/customers/test-customer").apply {
            assertEquals(HttpStatusCode.NotFound, status)
            assertEquals("", bodyAsText())
        }
    }

    @Test
    fun customerCanBeAdded() = testApplication {
        val client = createClient {
            install(ContentNegotiation){
                json()
            }
        }
        application {
            configureDatabases()
            configureSerialization()
        }
        client.post("/customers"){
            contentType(ContentType.Application.Json)
            setBody(CreateCustomerRequest(name="test-customer"))
        }.apply {
            assertEquals(HttpStatusCode.Created, status)
            assertEquals("", bodyAsText())
        }
        client.get("/customers/test-customer").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals(GetCustomerResponse(name="test-customer"), body(typeInfo<GetCustomerResponse>()))
        }
    }
}
