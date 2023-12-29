package com.edkohlwey.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureDatabases() {
    val database = Database.connect(
        url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
        user = "root",
        driver = "org.h2.Driver",
        password = ""
    )
    transaction(database) {
        SchemaUtils.create(Customers)
    }
    routing {
        // Create customer
        post<CreateCustomerRequest>("/customers") { request ->
            transaction {
                Customer.new { name = request.name }
            }
            call.respond(HttpStatusCode.Created)
        }
        // Read customer
        get("/customers/{name}") {
            val name = call.parameters["name"] ?: throw IllegalArgumentException("Must specify name")
            val customer = transaction {
                Customer.find(Customers.name eq name).firstOrNull()
            }
            if (customer != null) {
                call.respond(HttpStatusCode.OK, GetCustomerResponse(name = customer.name))
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
        // Delete customer
        delete("/customers/{name}") {
            val name = call.parameters["name"] ?: throw IllegalArgumentException("Invalid name")
            transaction {
                Customer.find(Customers.name eq name).firstOrNull()?.delete()
            }
            call.respond(HttpStatusCode.OK)
        }
    }
}

@Serializable
data class GetCustomerResponse(val name: String)

@Serializable
data class CreateCustomerRequest(val name: String)
