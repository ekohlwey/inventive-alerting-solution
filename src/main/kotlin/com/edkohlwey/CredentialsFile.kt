package com.edkohlwey

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.io.path.Path
import kotlin.io.path.readText

fun loadDefaultCredentials(): Credentials {
    val homeDirectory: String =
        System.getenv("HOME") ?: throw IllegalStateException("HOME environment variable not set")
    return Path("${homeDirectory}/inventive-app-credentials.json").readText().let {
        Json.decodeFromString<Credentials>(it)
    }
}

@Serializable
data class Credentials(val openAi: OpenAiCredentials, val looker: LookerCredentials)

@Serializable
data class OpenAiCredentials(val apiKey: String)

@Serializable
data class LookerCredentials(val baseUrl: String, val clientId: String, val clientSecret: String)
