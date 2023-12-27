package com.edkohlwey.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

fun Transaction.findDataSourceByCustomerAndName(customerName: String, datasourceName: String): DataSource? {
    val datasourceRow = Customers.innerJoin(DataSources)
        .select { Customers.name eq customerName and (DataSources.name eq datasourceName) }
        .firstOrNull()
        ?: return null
    return DataSource.wrapRow(datasourceRow)
}

fun Application.configureDatasourceRoutes(database: Database) {
    routing {
        post<CreateDatasourceRequest>("/customers/{customer_name}/datasources") { request ->
            val customerName =
                call.parameters["customer_name"] ?: throw IllegalArgumentException("Must specify customer name")
            val response = transaction(database) {
                val customer = Customer.find(Customers.name eq customerName).firstOrNull()
                    ?: return@transaction HttpStatusCode.NotFound
                DataSource.new {
                    name = request.name
                    url = request.url
                    username = request.username
                    password = request.password
                    owner = customer
                }
                return@transaction HttpStatusCode.Created
            }
            call.respond(response)
        }
        put<UpdateDatasourceRequest>("/customers/{customer_name}/datasources/{datasource_name}") { request ->
            val customerName =
                call.parameters["customer_name"] ?: throw IllegalArgumentException("Must specify customer name")
            val datasourceName =
                call.parameters["datasource_name"] ?: throw IllegalArgumentException("Must specify datasource name")
            val response = transaction(database) {
                val datasource = findDataSourceByCustomerAndName(customerName, datasourceName)
                    ?: return@transaction HttpStatusCode.NotFound
                datasource.apply {
                    url = request.url
                    username = request.username
                    password = request.password
                }
                return@transaction HttpStatusCode.OK
            }
            call.respond(response)
        }
        get("/customers/{customer_name}/datasources/{datasource_name}") {
            val customerName =
                call.parameters["customer_name"] ?: throw IllegalArgumentException("Must specify customer name")
            val datasourceName =
                call.parameters["datasource_name"] ?: throw IllegalArgumentException("Must specify datasource name")
            val (response, value) = transaction(database) {
                val datasource = findDataSourceByCustomerAndName(customerName, datasourceName)
                    ?: return@transaction HttpStatusCode.NotFound to null
                return@transaction HttpStatusCode.OK to GetDatasourceResponse(
                    name = datasource.name,
                    url = datasource.url,
                    username = datasource.username
                )
            }
            if (value == null) {
                call.respond(response)
            } else {
                call.respond(response, value)
            }
        }
    }
}

@Serializable
data class CreateDatasourceRequest(
    val name: String,
    val url: String,
    val username: String,
    val password: String
)

@Serializable
data class GetDatasourceResponse(
    val name: String,
    val url: String,
    val username: String
)

@Serializable
data class UpdateDatasourceRequest(
    val url: String,
    val username: String,
    val password: String
)