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
const val DEFAULT_RULE_NAME = "test-rule"
const val DEFAULT_RULE_DESCRIPTION = "test-description"
const val DEFAULT_RULE_DATASOURCE_CUSTOMER_NAME = "test-customer-2"
val DEFAULT_RULE_FILTERS = mapOf("filter1" to "value1")
val DEFAULT_RULE_PROPERTIES = listOf("property1")
val DEFAULT_RULE_KEYS = listOf("key1")
const val DEFAULT_RULE_DATASOURCE = DEFAULT_DATASOURCE_NAME
const val DEFAULT_RULE_TRIGGER_ON_NEW = true
const val DEFAULT_RULE_TRIGGER_ON_CHANGED = false
const val DEFAULT_RULE_TRIGGER_ON_REMOVED = false
const val DEFAULT_TRIGGER_NAME = "test-trigger"
const val DEFAULT_TRIGGER_EMAIL = "test-email"
const val DEFAULT_TRIGGER_DESCRIPTION = "test-description"
const val DEFAULT_EMAIL_PROMPT = "test-prompt"
const val DEFAULT_EMAIL_SCHEDULE = "0 13 * * *"
const val DEFAULT_MODEL_NAME = "test-model"
const val DEFAULT_VIEW_NAME = "test-view"
const val DEFAULT_DATASOURCE_TYPE = "LOOKER"

suspend fun HttpClient.createTestRule(
    customerName: String = DEFAULT_CUSTOMER_NAME,
    ruleName: String = DEFAULT_RULE_NAME,
    ruleDescription: String = DEFAULT_RULE_DESCRIPTION,
    filters: Map<String, String> = DEFAULT_RULE_FILTERS,
    properties: List<String> = DEFAULT_RULE_PROPERTIES,
    keys: List<String> = DEFAULT_RULE_KEYS,
    datasource: String = DEFAULT_RULE_DATASOURCE,
    triggerOnNew: Boolean = DEFAULT_RULE_TRIGGER_ON_NEW,
    triggerOnChanged: Boolean = DEFAULT_RULE_TRIGGER_ON_CHANGED,
    triggerOnRemoved: Boolean = DEFAULT_RULE_TRIGGER_ON_REMOVED,
    expectedStatus: HttpStatusCode = HttpStatusCode.Created,
    datasourceCustomer: String = DEFAULT_RULE_DATASOURCE_CUSTOMER_NAME
) {
    this.post("/customers/${customerName}/rules") {
        contentType(ContentType.Application.Json)
        setBody(
            CreateRuleRequest(
                name = ruleName,
                description = ruleDescription,
                filters = filters,
                properties = properties,
                keys = keys,
                owner = customerName,
                datasource = datasource,
                triggerOnNew = triggerOnNew,
                triggerOnChanged = triggerOnChanged,
                triggerOnRemoved = triggerOnRemoved,
                datasourceCustomer = datasourceCustomer
            )
        )
    }.apply {
        assertEquals(expectedStatus, status)
        assertEquals("", bodyAsText())
    }
}

suspend fun HttpClient.checkForTestRule(
    customerName: String = DEFAULT_CUSTOMER_NAME,
    ruleName: String = DEFAULT_RULE_NAME,
    expectedDescription: String = DEFAULT_RULE_DESCRIPTION,
    expectedFilters: Map<String, String> = DEFAULT_RULE_FILTERS,
    expectedProperties: List<String> = DEFAULT_RULE_PROPERTIES,
    expectedKeys: List<String> = DEFAULT_RULE_KEYS,
    expectedDatasource: String = DEFAULT_RULE_DATASOURCE,
    expectedTriggerOnNew: Boolean = DEFAULT_RULE_TRIGGER_ON_NEW,
    expectedTriggerOnChanged: Boolean = DEFAULT_RULE_TRIGGER_ON_CHANGED,
    expectedTriggerOnRemoved: Boolean = DEFAULT_RULE_TRIGGER_ON_REMOVED,
    expectedDataSourceCustomer: String = DEFAULT_RULE_DATASOURCE_CUSTOMER_NAME
) {
    this.get("/customers/${customerName}/rules/${ruleName}").apply {
        assertEquals(HttpStatusCode.OK, status)
        assertEquals(
            GetRuleResponse(
                name = ruleName,
                description = expectedDescription,
                filters = expectedFilters,
                properties = expectedProperties,
                keys = expectedKeys,
                owner = customerName,
                datasource = expectedDatasource,
                triggerOnNew = expectedTriggerOnNew,
                triggerOnChanged = expectedTriggerOnChanged,
                triggerOnRemoved = expectedTriggerOnRemoved,
                datasourceCustomer = expectedDataSourceCustomer,
            ), body(typeInfo<GetRuleResponse>())
        )
    }
}

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
    model: String = DEFAULT_MODEL_NAME,
    view: String = DEFAULT_VIEW_NAME,
    type: String = DEFAULT_DATASOURCE_TYPE,
    expectedStatus: HttpStatusCode = HttpStatusCode.Created,
) {
    this.post("/customers/${customerName}/datasources") {
        contentType(ContentType.Application.Json)
        setBody(
            CreateDatasourceRequest(
                name = datasourceName,
                url = datasourceUrl,
                username = datasourceUsername,
                password = datasourcePassword,
                model = model,
                view = view,
                type = type
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
    expectedUsername: String = DEFAULT_DATASOURCE_USERNAME,
    expectedType: String = DEFAULT_DATASOURCE_TYPE,
    expectedView: String = DEFAULT_VIEW_NAME,
    expectedModel: String = DEFAULT_MODEL_NAME,
) {
    this.get("/customers/${customerName}/datasources/${datasourceName}").apply {
        assertEquals(HttpStatusCode.OK, status)
        assertEquals(
            GetDatasourceResponse(
                name = datasourceName,
                url = expectedUrl,
                username = expectedUsername,
                type = expectedType,
                view = expectedView,
                model = expectedModel
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

suspend fun HttpClient.createTestEmailTrigger(
    customerName: String = DEFAULT_CUSTOMER_NAME,
    triggerName: String = DEFAULT_TRIGGER_NAME,
    triggerEmail: String = DEFAULT_TRIGGER_EMAIL,
    description: String = DEFAULT_TRIGGER_DESCRIPTION,
    prompt: String = DEFAULT_EMAIL_PROMPT,
    schedule: String = DEFAULT_EMAIL_SCHEDULE,
    rules: List<String> = listOf(DEFAULT_RULE_NAME),
    expectedStatus: HttpStatusCode = HttpStatusCode.Created
) {
    this.post("/customers/${customerName}/triggers") {
        contentType(ContentType.Application.Json)
        setBody(
            CreateTriggerRequest(
                name = triggerName,
                description = description,
                rules = rules,
                schedule = schedule,
                emailTrigger = CreateEmailTriggerRequest(
                    email = triggerEmail,
                    prompt = prompt,
                )
            )
        )
    }.apply {
        assertEquals(expectedStatus, status)
    }
}

suspend fun HttpClient.checkForTestTrigger(
    customerName: String = DEFAULT_CUSTOMER_NAME,
    triggerName: String = DEFAULT_TRIGGER_NAME,
    triggerEmail: String = DEFAULT_TRIGGER_EMAIL,
    description: String = DEFAULT_TRIGGER_DESCRIPTION,
    rules: List<String> = listOf(DEFAULT_RULE_NAME),
    prompt: String = DEFAULT_EMAIL_PROMPT,
    schedule: String = DEFAULT_EMAIL_SCHEDULE,
    expectedStatus: HttpStatusCode = HttpStatusCode.OK
) {
    this.get("/customers/${customerName}/triggers/${triggerName}").apply {
        assertEquals(expectedStatus, status)
        if (expectedStatus == HttpStatusCode.OK) {
            assertEquals(
                GetTriggerResponse(
                    name = triggerName,
                    description = description,
                    rules = rules,
                    schedule = schedule,
                    emailTrigger = GetEmailTriggerResponse(
                        email = triggerEmail,
                        prompt = prompt,
                    )
                ), body(typeInfo<GetTriggerResponse>())
            )
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
        RuleStates.deleteAll()
        EmailTriggers.deleteAll()
        TriggerRules.deleteAll()
        Triggers.deleteAll()
        Rules.deleteAll()
        CustomerDatasources.deleteAll()
        DataSources.deleteAll()
        Customers.deleteAll()
    }
}