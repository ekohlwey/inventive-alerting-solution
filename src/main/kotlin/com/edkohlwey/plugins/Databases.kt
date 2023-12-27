package com.edkohlwey.plugins

import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.mapstruct.Mapper

fun Application.configureDatabases() {
    val database = Database.connect(
        url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
        user = "root",
        driver = "org.h2.Driver",
        password = ""
    )
    val customerService = CustomerService(database)
    val customerMapper: CustomerMapper = CustomerMapperImpl()
    routing {
        // Create customer
        post<CreateCustomerRequest>("/customers") { request ->
            customerService.create(customerMapper.toModel(request))
            call.respond(HttpStatusCode.Created)
        }
        // Read customer
        get("/customers/{name}") {
            val name = call.parameters["name"] ?: throw IllegalArgumentException("Must specify name")
            val customer = customerService.read(name)
            if (customer != null) {
                call.respond(HttpStatusCode.OK, customerMapper.toGetDto(customer))
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
        // Delete customer
        delete("/customers/{name}") {
            val name = call.parameters["name"] ?: throw IllegalArgumentException("Invalid name")
            customerService.delete(name)
            call.respond(HttpStatusCode.OK)
        }
    }
}


@Serializable
data class GetCustomerResponse(val name: String)

@Serializable
data class CreateCustomerRequest(val name: String)

@Mapper
interface CustomerMapper {
    fun toModel(customer: CreateCustomerRequest): Customer
    fun toGetDto(customer: Customer): GetCustomerResponse
}