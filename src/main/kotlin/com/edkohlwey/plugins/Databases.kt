package com.edkohlwey.plugins

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun configureDatabases(): Database {
    val database = Database.connect(
        url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
        user = "root",
        driver = "org.h2.Driver",
        password = ""
    )
    transaction(database) {
        SchemaUtils.create(Customers)
        SchemaUtils.create(DataSources)
        SchemaUtils.create(CustomerDatasources)
        SchemaUtils.create(Rules)
        SchemaUtils.create(Triggers)
        SchemaUtils.create(TriggerRules)
        SchemaUtils.create(EmailTriggers)
        SchemaUtils.create(RuleStates)
    }
    return database
}
