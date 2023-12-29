package com.edkohlwey.plugins

import com.looker.rtl.AuthSession
import com.looker.sdk.ApiSettings
import com.looker.sdk.LookerSDK
import com.looker.sdk.WriteQuery
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.*

class LookerDataSourceConnection(val baseUrl: String, val clientId: String, val secret: String) : DataSourceConnection {
    private val apiSettings = ApiSettings {
        mapOf(
            "base_url" to baseUrl, "client_id" to clientId, "client_secret" to secret
        )
    }

    override fun checkForData(
        model: String, view: String, filters: Map<String, String>, fields: List<String>
    ): List<Map<String, String>> {
        val authSession = AuthSession(apiSettings)
        try {
            val sdk = LookerSDK(authSession)
            val query = sdk.run_inline_query(
                result_format = "json", body = WriteQuery(
                    model = model, view = view, fields = fields.toTypedArray(), filters = filters
                ), cache = true, limit = 10
            )
            val results = sdk.ok<String>(query)
            return Json.decodeFromString(ListSerializer(StringifyingSerializer), results)
        } finally {
            if (authSession.isAuthenticated()) {
                authSession.logout()
            }
        }
    }

}

object StringifyingSerializer :
    JsonTransformingSerializer<Map<String, String>>(MapSerializer(String.serializer(), String.serializer())) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        if (element is JsonObject) {
            val transformedMap = element.mapValues { (_, value) ->
                when (value) {
                    is JsonPrimitive -> JsonPrimitive(value.content)
                    else -> value
                }
            }
            return JsonObject(transformedMap)
        }
        return element
    }
}