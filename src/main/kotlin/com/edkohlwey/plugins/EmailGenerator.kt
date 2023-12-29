package com.edkohlwey.plugins

import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.http.Timeout
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.edkohlwey.OpenAiCredentials
import com.hubspot.jinjava.Jinjava
import com.hubspot.jinjava.lib.filter.ToJsonFilter
import io.ktor.util.logging.*
import kotlin.time.Duration.Companion.seconds

interface EmailGenerator {
    suspend fun generateEmail(emailJobSpec: EmailJobSpec, triggerEvents: List<TriggerEvent>): String
}

class OpenAiEmailGenerator(val credentials: OpenAiCredentials) : EmailGenerator {

    private val LOGGER = KtorSimpleLogger(OpenAiEmailGenerator::class.qualifiedName!!)
    val jinjava = Jinjava().apply {
        globalContext.registerFilter(ToJsonFilter())
    }

    override suspend fun generateEmail(emailJobSpec: EmailJobSpec, triggerEvents: List<TriggerEvent>): String {
        val openai = OpenAI(
            token = credentials.apiKey,
            timeout = Timeout(socket = 60.seconds),
        )
        val templatedPrompt = jinjava.render(
            emailJobSpec.prompt, mapOf(
                "triggerEvents" to triggerEvents,
                "email" to emailJobSpec.email,
            )
        )
        LOGGER.debug(templatedPrompt)
        val chatCompletionRequest = ChatCompletionRequest(
            model = ModelId("gpt-3.5-turbo-1106"), messages = listOf(
                ChatMessage(
                    role = ChatRole.User, content = jinjava.render(
                        emailJobSpec.prompt, mapOf(
                            "triggerEvents" to triggerEvents,
                            "email" to emailJobSpec.email,
                        )
                    )
                )
            )
        )
        val chatResponse = openai.chatCompletion(chatCompletionRequest)
        return chatResponse.choices.first().message.content ?: throw EmailGenerationException("No response from OpenAI")
    }

    class EmailGenerationException(message: String) : Exception(message)

}