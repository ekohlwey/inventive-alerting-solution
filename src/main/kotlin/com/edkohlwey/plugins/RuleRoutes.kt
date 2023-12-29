package com.edkohlwey.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureRuleRoutes(database: Database) {
    routing {
        post<CreateRuleRequest>("/customers/{customer_name}/rules") { request ->
            val response = transaction(database) {
                val owner = Customer.findByName(request.owner) ?: return@transaction HttpStatusCode.NotFound
                val datasource =
                    DataSource.findDataSourceByCustomerAndName(request.datasourceCustomer, request.datasource)
                        ?: return@transaction HttpStatusCode.NotFound
                Rule.new {
                    name = request.name
                    description = request.description
                    filters = request.filters
                    properties = request.properties
                    keys = request.keys
                    this.owner = owner
                    this.datasource = datasource
                    triggerOnNew = request.triggerOnNew
                    triggerOnChanged = request.triggerOnChanged
                    triggerOnRemoved = request.triggerOnRemoved
                }
                return@transaction HttpStatusCode.Created
            }
            call.respond(response)
        }
        get("/customers/{customer_name}/rules/{rule_name}") {
            val customerName =
                call.parameters["customer_name"] ?: throw IllegalArgumentException("Must specify customer name")
            val ruleName = call.parameters["rule_name"] ?: throw IllegalArgumentException("Must specify rule name")
            val (status, response) = transaction(database) {
                val rule = Rule.findRuleByCustomerAndName(customerName, ruleName)
                    ?: return@transaction HttpStatusCode.NotFound to null
                return@transaction HttpStatusCode.OK to GetRuleResponse(
                    name = rule.name,
                    description = rule.description,
                    filters = rule.filters,
                    properties = rule.properties,
                    keys = rule.keys,
                    owner = rule.owner.name,
                    datasource = rule.datasource.name,
                    triggerOnNew = rule.triggerOnNew,
                    triggerOnChanged = rule.triggerOnChanged,
                    triggerOnRemoved = rule.triggerOnRemoved,
                    datasourceCustomer = rule.datasource.owner.name
                )
            }
            if (response != null) {
                call.respond(
                    status, response
                )
            }
            call.respond(status)
        }
    }
}

@Serializable
data class GetRuleResponse(
    val name: String,
    val description: String,
    val filters: Map<String, String>,
    val properties: List<String>,
    val keys: List<String>,
    val owner: String,
    val datasource: String,
    val datasourceCustomer: String,
    val triggerOnNew: Boolean,
    val triggerOnChanged: Boolean,
    val triggerOnRemoved: Boolean
)

@Serializable
data class CreateRuleRequest(
    val name: String,
    val description: String,
    val filters: Map<String, String>,
    val properties: List<String>,
    val keys: List<String>,
    val owner: String,
    val datasource: String,
    val datasourceCustomer: String,
    val triggerOnNew: Boolean,
    val triggerOnChanged: Boolean,
    val triggerOnRemoved: Boolean
)
