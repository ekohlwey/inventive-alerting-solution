package com.edkohlwey.plugins

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select

object Triggers : IntIdTable() {
    val name = varchar("name", length = 128).uniqueIndex()
    val description = varchar("description", length = 512)
    val owner = reference("owner", Customers)
    val schedule = text("schedule")
}

object TriggerRules : IntIdTable() {
    val trigger = reference("trigger", Triggers)
    val rule = reference("rule", Rules)
}

object EmailTriggers : IntIdTable() {
    val trigger = reference("trigger", Triggers, onDelete = ReferenceOption.CASCADE).uniqueIndex()
    val email = varchar("email", length = 128)
    val prompt = text("prompt")
}

class EmailTrigger(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<EmailTrigger>(EmailTriggers)

    var trigger by Trigger referencedOn EmailTriggers.trigger
    var email by EmailTriggers.email
    var prompt by EmailTriggers.prompt
}

class Trigger(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Trigger>(Triggers) {
        fun findTriggerByCustomerAndName(customerName: String, triggerName: String): Trigger? {
            val triggerRow = Customers.innerJoin(Triggers)
                .select { Customers.name eq customerName and (Triggers.name eq triggerName) }
                .firstOrNull()
                ?: return null
            return Trigger.wrapRow(triggerRow)
        }
    }

    var name by Triggers.name
    var description by Triggers.description
    var rules by Rule via TriggerRules
    var owner by Customer referencedOn Triggers.owner
    var schedule by Triggers.schedule
    val emailTrigger: EmailTrigger? by EmailTrigger optionalBackReferencedOn EmailTriggers.trigger
}