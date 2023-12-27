package com.edkohlwey.plugins

import io.ktor.server.application.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureDatabases(): Database {
    val database = Database.connect(
        url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
        user = "root",
        driver = "org.h2.Driver",
        password = ""
    )
    transaction(database) {
        SchemaUtils.create(Customers)
    }
    return database
}

@Serializable
data class GetCustomerResponse(val name: String)

@Serializable
data class CreateCustomerRequest(val name: String)
