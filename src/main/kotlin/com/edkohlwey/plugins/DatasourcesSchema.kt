package com.edkohlwey.plugins

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable


object DataSources : IntIdTable() {
    val name = varchar("name", length = 128).uniqueIndex()
    val url = varchar("url", length = 512)
    val username = varchar("username", length = 128)
    val password = varchar("password", length = 128)
    val owner = reference("owner", Customers)
}

class DataSource(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<DataSource>(DataSources)

    var name by DataSources.name
    var url by DataSources.url
    var username by DataSources.username
    var password by DataSources.password
    var owner by Customer referencedOn DataSources.owner
}