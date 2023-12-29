package com.edkohlwey.plugins

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object Customers : IntIdTable() {
    val name = varchar("name", length = 128)
}

class Customer(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Customer>(Customers)

    var name by Customers.name
}
