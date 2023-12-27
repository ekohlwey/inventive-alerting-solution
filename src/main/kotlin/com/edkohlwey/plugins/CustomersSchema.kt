package com.edkohlwey.plugins

import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class Customer(val name: String)
class CustomerService(private val database: Database) {
    object Customers : Table() {
        val id = integer("id").autoIncrement()
        val name = varchar("name", length = 128)
        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.create(Customers)
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    suspend fun create(user: Customer): Unit = dbQuery {
        Customers.insert {
            it[name] = user.name
        }
    }

    suspend fun read(name: String): Customer? {
        return dbQuery {
            Customers.select { Customers.name eq name }
                .map { Customer(it[Customers.name]) }
                .singleOrNull()
        }
    }

}
