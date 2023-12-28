package com.edkohlwey.plugins

import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.json.json
import org.jetbrains.exposed.sql.select

object RuleStates : IntIdTable() {
    val rule = reference("rule", Rules)
    val customer = reference("customer", Customers)
    val keyValues = json<Map<String, String>>("key_values", Json.Default).index()
    val allValues = json<Map<String, String>>("all_values", Json.Default)
    val lastUpdated = datetime("last_updated")
}

class RuleState(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<RuleState>(RuleStates) {
        fun findByCustomerAndRuleNames(customerName: String, ruleName: String): RuleState? {
            val ruleStateRow = Customers.innerJoin(Rules).innerJoin(RuleStates)
                .select { Customers.name eq customerName and (Rules.name eq ruleName) }
                .firstOrNull()
                ?: return null
            return RuleState.wrapRow(ruleStateRow)
        }
    }

    var rule by Rule referencedOn RuleStates.rule
    var customer by Customer referencedOn RuleStates.customer
    var keyValues by RuleStates.keyValues
    var allValues by RuleStates.allValues
    var lastUpdated by RuleStates.lastUpdated
}
