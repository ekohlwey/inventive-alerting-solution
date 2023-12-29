package com.edkohlwey.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureTriggerRoutes(database: Database) {
    routing {
        post<CreateTriggerRequest>("/customers/{customer_name}/triggers") { request ->
            val customerName =
                call.parameters["customer_name"] ?: throw IllegalArgumentException("Must specify customer name")
            val response = transaction(database) {
                val owner = Customer.findByName(customerName) ?: return@transaction HttpStatusCode.NotFound
                val rules = request.rules.map { Rule.findRuleByCustomerAndName(customerName, it) }
                if (rules.any { it == null }) {
                    return@transaction HttpStatusCode.NotFound
                }
                val trigger = Trigger.new {
                    name = request.name
                    description = request.description
                    this.owner = owner
                    this.rules = SizedCollection(rules.filterNotNull())
                }
                if (request.emailTrigger != null) {
                    EmailTrigger.new {
                        this.trigger = trigger
                        email = request.emailTrigger.email
                        prompt = request.emailTrigger.prompt
                        schedule = request.emailTrigger.schedule
                    }
                }
                return@transaction HttpStatusCode.Created
            }
            call.respond(response)
        }

        get("/customers/{customer_name}/triggers/{trigger_name}") {
            val customerName =
                call.parameters["customer_name"] ?: throw IllegalArgumentException("Must specify customer name")
            val triggerName =
                call.parameters["trigger_name"] ?: throw IllegalArgumentException("Must specify trigger name")
            val (code, response) = transaction(database) {
                val trigger = Trigger.findTriggerByCustomerAndName(customerName, triggerName)
                    ?: return@transaction HttpStatusCode.NotFound to null
                return@transaction HttpStatusCode.OK to trigger.toGetTriggerResponse()
            }
            if (response != null) {
                call.respond(code, response)
            } else {
                call.respond(code)
            }
        }
    }
}

@Serializable
data class CreateTriggerRequest(
    val name: String,
    val description: String,
    val rules: List<String>,
    val emailTrigger: CreateEmailTriggerRequest?
)

@Serializable
data class CreateEmailTriggerRequest(
    val email: String,
    val prompt: String,
    val schedule: String
)

@Serializable
data class GetTriggerResponse(
    val name: String,
    val description: String,
    val rules: List<String>,
    val emailTrigger: GetEmailTriggerResponse?
)

@Serializable
data class GetEmailTriggerResponse(
    val email: String,
    val prompt: String,
    val schedule: String
)

fun Trigger.toGetTriggerResponse(): GetTriggerResponse {
    val emailTrig = this.emailTrigger
    return GetTriggerResponse(
        name = this.name,
        description = this.description,
        rules = this.rules.map { it.name },
        emailTrigger = emailTrig?.let {
            GetEmailTriggerResponse(
                email = it.email,
                prompt = it.prompt,
                schedule = it.schedule
            )
        }
    )
}