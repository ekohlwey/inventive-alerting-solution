package com.edkohlwey

import com.edkohlwey.plugins.*
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.Test
import kotlin.test.assertEquals

class JobSchedulerTest {

    private class FakeEmailSender : EmailSender {
        val sentEmails = mutableListOf<String>()
        override fun sendEmail(email: String) {
            sentEmails.add(email)
        }
    }

    private class FakeEmailGenerator : EmailGenerator {
        var id = 0
        override suspend fun generateEmail(emailJobSpec: EmailJobSpec, triggerEvents: List<TriggerEvent>): String {
            return "email number ${id++}"
        }

    }

    private class FakeRuleStateEngine : RuleStateEngine {
        override fun checkRules(customer: String, rules: List<RuleJobSpec>): List<TriggerEvent> {
            return listOf(
                TriggerEvent(
                    TriggerKind.CHANGED,
                    "test datasource",
                    "test key",
                    mapOf("test field" to "test value")
                )
            )
        }


    }

    @Test
    fun jobsAreAddedFromDatabaseAndExecute() {
        val database = configureDatabases()
        val fakeEmailSender = FakeEmailSender()
        val fakeEmailGenerator = FakeEmailGenerator()
        val jobScheduler = JobScheduler(database, FakeRuleStateEngine(), fakeEmailGenerator, fakeEmailSender, 10)
        jobScheduler.start()
        assertEquals(0, jobScheduler.monitorNumJobs())
        transaction(database) {
            val customer = Customer.new { name = "test customer" }
            val datasource = DataSource.new {
                name = "test datasource"
                url = "test url"
                username = "test username"
                password = "test password"
                owner = customer
                model = "test model"
                view = "test view"
                type = DataSourceType.LOOKER.ordinal
            }
            val rule = Rule.new {
                owner = customer
                name = "test rule"
                description = "test description"
                this.datasource = datasource
                filters = mapOf("test filter" to "test value")
                keys = listOf("test key")
                properties = listOf("test field")
                triggerOnNew = true
                triggerOnChanged = true
                triggerOnRemoved = true
            }
            val trigger = Trigger.new {
                this.rules = SizedCollection(listOf(rule))
                this.schedule = "* * * ? * *" // every second
                this.name = "test-trigger"
                this.owner = customer
                this.description = "Test trigger"
            }
            EmailTrigger.new {
                this.trigger = trigger
                email = "test_email@foo.com"
                this.prompt = "prompt"
            }
        }
        Thread.sleep(100)
        assertEquals(1, jobScheduler.monitorNumJobs())
        Thread.sleep(1001)
        assertEquals(1, fakeEmailSender.sentEmails.size)
        Thread.sleep(1001)
        assertEquals(2, fakeEmailSender.sentEmails.size)
        jobScheduler.stop()
    }
}