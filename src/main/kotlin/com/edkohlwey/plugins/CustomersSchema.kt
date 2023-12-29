package com.edkohlwey.plugins

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

object Customers : IntIdTable() {
    val name = varchar("name", length = 128).uniqueIndex()
}

object CustomerDatasources : IntIdTable() {
    val customer = reference("customer", Customers)
    val datasource = reference("datasource", DataSources)
}

class CustomerDatasource(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<CustomerDatasource>(CustomerDatasources)

    var customer by Customer referencedOn CustomerDatasources.customer
    var datasource by DataSource referencedOn CustomerDatasources.datasource
}

class Customer(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Customer>(Customers) {
        fun findByName(name: String): Customer? {
            return Customer.find(Customers.name eq name).firstOrNull()
        }
    }

    var name by Customers.name
    var datasources by DataSource via CustomerDatasources
}

