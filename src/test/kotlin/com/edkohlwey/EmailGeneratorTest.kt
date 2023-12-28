package com.edkohlwey

import com.edkohlwey.plugins.*
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

class EmailGeneratorTest {

    val credentials = loadDefaultCredentials()

    @Test
    fun emailIsGeneratedForResults() {
        val generator = OpenAiEmailGenerator(credentials.openAi)
        val emailJobSpec = emailJobSpecWithPrompt(
            """
            Your are an assistant to a marketing executive that works for a high-end clothing retailer. Every day you 
            receive a list of products that have either changed in price above $200, are new and are above $200, or
            have dropped below $200.
            
            The raw data is provided json-formatted. A null value for the oldValues field indicates that the product has 
            no prior history.
            
            The raw data for the price changes are as follows:
            {% for triggerEvent in triggerEvents %}
            {{ triggerEvent | tojson() }}
            
            {% endfor %}
            
            Please summarize this information in a way that is easy for the marketing executive to understand. Make sure
            that all the price change information is clearly reflected in bulleted form. Include a section for helpful
            marketing sections at the end.
            """.trimIndent()
        )
        val triggerEvents = triggerEvents()
        val result = runBlocking {
            generator.generateEmail(emailJobSpec, triggerEvents)
        }
        print(result)
    }

    private fun triggerEvents() = listOf(
        TriggerEvent(
            TriggerKind.NEW,
            "test datasource",
            "test rule",
            mapOf(
                "order_items.sale_price" to "293.3299865722656",
                "products.category" to "Pants",
                "products.item_name" to "True Religion Men's Ricky Straight Corduroy Pant",
            )
        ),
        TriggerEvent(
            TriggerKind.CHANGED,
            "test datasource",
            "test rule",
            mapOf(
                "order_items.sale_price" to "219",
                "products.category" to "Pants",
                "products.item_name" to "Eddie Bauer First Ascent Heyburn 2.0 Pants",
            ),
            mapOf(
                "order_items.sale_price" to "223",
                "products.category" to "Pants",
                "products.item_name" to "Eddie Bauer First Ascent Heyburn 2.0 Pants",
            )
        )
    )

    private fun emailJobSpecWithPrompt(prompt: String) = EmailJobSpec(
        "test customer",
        "test trigger",
        "test email",
        prompt,
        "test datasource",
        listOf(
            RuleJobSpec(
                "test rule",
                "test description",
                DataSourceJobSpec(
                    "test datasource",
                    "test url",
                    "test username",
                    "test password",
                    "test model",
                    "test view",
                    "LOOKER"
                ),
                mapOf("test filter" to "test value"),
                listOf("test field"),
                setOf("test key"),
                true,
                true,
                true
            )
        ),
        123
    )


}