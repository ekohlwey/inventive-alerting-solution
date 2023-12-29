package com.edkohlwey

import com.edkohlwey.plugins.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals


const val TEST_FIELD_1 = "test_field_1"
const val TEST_FIELD_2 = "test_field_2"
const val TEST_KEYVALUE = "test_key_value"
const val TEST_OLD_FIELD_VALUE = "test_old_value"
const val TEST_NEW_FIELD_VALUE = "test_new_value"
const val TEST_RULE = "test_rule"
const val TEST_CUSTOMER = "test_customer"
const val TEST_DATASOURCE = "test_datasource"

class RuleStateEngineTest {

    class FakeDataSourceConnection(val value: String?) : DataSourceConnection {
        override fun checkForData(
            model: String,
            view: String,
            filters: Map<String, String>,
            fields: List<String>
        ): List<Map<String, String>> {
            if (value != null) {
                return listOf(mapOf(TEST_FIELD_1 to TEST_KEYVALUE, TEST_FIELD_2 to value))
            } else {
                return emptyList()
            }
        }
    }

    class FakeDataSourceConnectionFactory(val value: String?) : DataSourceConnectionFactory {
        override fun createDataSource(datasource: DataSourceJobSpec): DataSourceConnection {
            return FakeDataSourceConnection(value)
        }
    }

    @Test
    fun triggersOnChanged() {
        val database = configureDatabases()
        clearDatabases(database)
        val engine = DatabaseRuleStateEngine(database, FakeDataSourceConnectionFactory(TEST_NEW_FIELD_VALUE))
        addTestData(database)
        val triggerEvents = engine.checkRules(
            TEST_CUSTOMER, ruleJobSpec()
        )
        assertEquals(
            listOf(
                TriggerEvent(
                    TriggerKind.CHANGED,
                    TEST_CUSTOMER,
                    TEST_RULE,
                    mapOf(TEST_FIELD_1 to TEST_KEYVALUE, TEST_FIELD_2 to TEST_NEW_FIELD_VALUE),
                    mapOf(TEST_FIELD_1 to TEST_KEYVALUE, TEST_FIELD_2 to TEST_OLD_FIELD_VALUE)
                )
            ), triggerEvents
        )
    }

    @Test
    fun triggersOnNew() {
        val database = configureDatabases()
        clearDatabases(database)
        val engine = DatabaseRuleStateEngine(database, FakeDataSourceConnectionFactory(TEST_OLD_FIELD_VALUE))
        val triggerEvents = engine.checkRules(
            TEST_CUSTOMER, ruleJobSpec()
        )
        assertEquals(
            listOf(
                TriggerEvent(
                    TriggerKind.NEW,
                    TEST_CUSTOMER,
                    TEST_RULE,
                    mapOf(TEST_FIELD_1 to TEST_KEYVALUE, TEST_FIELD_2 to TEST_OLD_FIELD_VALUE)
                )
            ), triggerEvents
        )
    }

    @Test
    fun triggersOnRemoved() {
        val database = configureDatabases()
        clearDatabases(database)
        addTestData(database)
        val engine = DatabaseRuleStateEngine(database, FakeDataSourceConnectionFactory(null))
        val triggerEvents = engine.checkRules(
            TEST_CUSTOMER, ruleJobSpec()
        )
        assertEquals(
            listOf(
                TriggerEvent(
                    TriggerKind.REMOVED,
                    TEST_CUSTOMER,
                    TEST_RULE,
                    null,
                    mapOf(TEST_FIELD_1 to TEST_KEYVALUE, TEST_FIELD_2 to TEST_OLD_FIELD_VALUE)
                )
            ), triggerEvents
        )
    }

    private fun ruleJobSpec(): List<RuleJobSpec> {
        return listOf(
            RuleJobSpec(
                name = TEST_RULE,
                description = "test description",
                datasource = DataSourceJobSpec(
                    name = TEST_DATASOURCE,
                    url = "test url",
                    username = "test username",
                    password = "test password",
                    model = "test model",
                    view = "test view",
                    type = "LOOKER"
                ),
                filters = mapOf(TEST_FIELD_1 to "test expression"),
                fields = listOf(TEST_FIELD_1, TEST_FIELD_2),
                keys = setOf(TEST_FIELD_1),
                triggerOnNew = true,
                triggerOnChanged = true,
                triggerOnRemoved = true
            )
        )
    }

    private fun addTestData(database: Database) {
        transaction(database) {
            val customer = Customer.new { name = TEST_CUSTOMER }
            val datasource = DataSource.new {
                name = TEST_DATASOURCE
                url = "test url"
                username = "test username"
                password = "test password"
                owner = customer
                model = "test model"
                view = "test view"
                type = DataSourceType.LOOKER.ordinal
            }
            val rule = Rule.new {
                name = TEST_RULE
                description = "test description"
                this.datasource = datasource
                filters = mapOf(TEST_FIELD_1 to "test expression")
                properties = listOf(TEST_FIELD_1)
                keys = listOf(TEST_FIELD_1)
                triggerOnNew = true
                triggerOnChanged = true
                triggerOnRemoved = true
                owner = customer
            }
            RuleState.new {
                this.rule = rule
                this.customer = customer
                this.allValues = mapOf(TEST_FIELD_1 to TEST_KEYVALUE, TEST_FIELD_2 to TEST_OLD_FIELD_VALUE)
                this.keyValues = mapOf(TEST_FIELD_1 to TEST_KEYVALUE)
                this.lastUpdated = LocalDateTime.now().minusHours(1)
            }
        }
    }
}