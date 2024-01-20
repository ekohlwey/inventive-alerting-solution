package com.edkohlwey

import com.edkohlwey.plugins.LookerDataSourceConnection
import kotlin.test.Test

class LookerDataSourceConnectionTest {
    val credentials = loadDefaultCredentials().looker

    @Test
    fun testLookerDataSource() {
        val lookerDataSourceConnection =
            LookerDataSourceConnection(credentials.baseUrl, credentials.clientId, credentials.clientSecret)
        val result = lookerDataSourceConnection.checkForData(
            "thelook_partner",
            "order_items",
            mapOf("order_items.sale_price" to ">200", "products.category" to "Pants"),
            listOf("order_items.sale_price", "products.category", "products.item_name")
        )
        assert(result.isNotEmpty())
    }
}