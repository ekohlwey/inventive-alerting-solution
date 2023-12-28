package com.edkohlwey.plugins

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select


enum class DataSourceType {
    LOOKER
}

object DataSources : IntIdTable() {
    val name = varchar("name", length = 128).uniqueIndex()
    val url = varchar("url", length = 512)
    val username = varchar("username", length = 128)
    val password = varchar("password", length = 128)
    val owner = reference("owner", Customers)
    val model = varchar("model", length = 128)
    val view = varchar("view", length = 128)
    val type = integer("type")
}

class DataSource(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<DataSource>(DataSources) {
        fun findDataSourceByCustomerAndName(customerName: String, datasourceName: String): DataSource? {
            val datasourceRow = Customers.innerJoin(DataSources)
                .select { Customers.name eq customerName and (DataSources.name eq datasourceName) }
                .firstOrNull()
                ?: return null
            return DataSource.wrapRow(datasourceRow)
        }
    }

    var name by DataSources.name
    var url by DataSources.url
    var username by DataSources.username
    var password by DataSources.password
    var owner by Customer referencedOn DataSources.owner
    var model by DataSources.model
    var view by DataSources.view
    var type by DataSources.type
}