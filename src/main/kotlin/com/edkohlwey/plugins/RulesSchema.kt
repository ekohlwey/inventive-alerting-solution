package com.edkohlwey.plugins


import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.json.json
import org.jetbrains.exposed.sql.select

object Rules : IntIdTable() {
    val name = varchar("name", length = 128).uniqueIndex()
    val description = varchar("description", length = 512)
    val filters = json<Map<String, String>>("filters", Json.Default)
    val properties = json<List<String>>("properties", Json.Default)
    val keys = json<List<String>>("keys", Json.Default)
    val owner = reference("owner", Customers)
    val datasource = reference("datasource", DataSources)
    val triggerOnNew = bool("trigger_on_new")
    val triggerOnChanged = bool("trigger_on_changed")
    val triggerOnRemoved = bool("trigger_on_removed")
}

class Rule(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Rule>(Rules) {
        fun findRuleByCustomerAndName(customerName: String, ruleName: String): Rule? {
            val datasourceRow =
                Customers.innerJoin(Rules).select { Customers.name eq customerName and (Rules.name eq ruleName) }
                    .firstOrNull() ?: return null
            return Rule.wrapRow(datasourceRow)
        }
    }

    var name by Rules.name
    var description by Rules.description
    var filters by Rules.filters
    var properties by Rules.properties
    var keys by Rules.keys
    var owner by Customer referencedOn Rules.owner
    var datasource by DataSource referencedOn Rules.datasource
    var triggerOnNew by Rules.triggerOnNew
    var triggerOnChanged by Rules.triggerOnChanged
    var triggerOnRemoved by Rules.triggerOnRemoved
}

