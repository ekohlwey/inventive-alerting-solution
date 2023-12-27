package com.edkohlwey.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureCustomerRoutes(database: Database) {
    routing {
        // Create customer
        post<CreateCustomerRequest>("/customers") { request ->
            transaction(database) {
                Customer.new { name = request.name }
            }
            call.respond(HttpStatusCode.Created)
        }
        // Read customer
        get("/customers/{name}") {
            val name = call.parameters["name"] ?: throw IllegalArgumentException("Must specify name")
            val customer = transaction(database) {
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
            transaction(database) {
                Customer.find(Customers.name eq name).firstOrNull()?.delete()
            }
            call.respond(HttpStatusCode.OK)
        }
    }
}