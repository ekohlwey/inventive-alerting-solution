package com.edkohlwey.plugins

import com.cronutils.model.Cron
import com.cronutils.model.CronType
import com.cronutils.model.definition.CronDefinitionBuilder
import com.cronutils.model.time.ExecutionTime
import com.cronutils.parser.CronParser
import com.edkohlwey.Credentials
import io.ktor.server.application.*
import io.ktor.server.application.hooks.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Join
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.collections.set
import kotlin.jvm.optionals.getOrNull
import kotlin.time.Duration.Companion.minutes


/**
 * This plugin and corresponding class reads the database for triggers and schedules trigger execution according to the
 * cron expression for the trigger.
 */
@ExperimentalCoroutinesApi
fun Application.jobSchedulerPlugin(
    database: Database,
    credentials: Credentials,
    ruleStateEngine: RuleStateEngine = DatabaseRuleStateEngine(database),
    emailGenerator: EmailGenerator = OpenAiEmailGenerator(credentials.openAi),
    emailSender: EmailSender = LogEmailSender(),
    scanTimeMillis: Long = 5.minutes.inWholeMilliseconds
) = createApplicationPlugin("JobScheduler") {
    val jobScheduler = JobScheduler(database, ruleStateEngine, emailGenerator, emailSender, scanTimeMillis)
    on(MonitoringEvent(ApplicationStarting)) { _ ->
        jobScheduler.start()
    }
    on(MonitoringEvent(ApplicationStopping)) { _ ->
        jobScheduler.stop()
    }
}

@ExperimentalCoroutinesApi
class JobScheduler(
    private val database: Database,
    private val ruleStateEngine: RuleStateEngine,
    private val emailGenerator: EmailGenerator,
    private val emailSender: EmailSender,
    private val scanTimeMillis: Long
) {
    private val dispatcher = Dispatchers.IO.limitedParallelism(100)
    private val schedulerScope = CoroutineScope(dispatcher)
    private var running: Boolean = true
    private val jobMap = mutableMapOf<Int, JobSpecAndRunningJob>()
    private var scannerJob: Job? = null
    private val jobMapMutex = Mutex()

    private suspend fun readNewTriggers(): Iterable<JobSpec> {
        val jobIds = jobMapMutex.withLock { jobMap.keys.toList() }
        return transaction(database) {
            Join(EmailTriggers).innerJoin(Triggers).select { Triggers.id notInList jobIds }
                .map { EmailTrigger.wrapRow(it).toJobSpec() }
            // add more trigger types here
        }
    }

    fun start() {
        setupNewJobScanner()
    }

    fun monitorNumJobs(): Int {
        return runBlocking { jobMapMutex.withLock { jobMap.size } }
    }

    private fun setupNewJobScanner() {
        scannerJob = schedulerScope.launch {
            scanforNewJobs()
        }
    }

    private suspend fun scanforNewJobs() {
        while (running) {
            readNewTriggers().forEach { jobSpec ->
                schedulerScope.launch {
                    startJob(jobSpec)
                }
            }
            delay(scanTimeMillis)
        }
    }

    fun stop() {
        running = false
        runBlocking { schedulerScope.launch { dispatcher.job.cancelAndJoin() } }
    }


    private suspend fun startJob(jobSpec: JobSpec) {
        withContext(dispatcher) {
            val job = launch {
                executeJob(jobSpec)
            }
            jobMapMutex.withLock {
                jobMap[jobSpec.id] = JobSpecAndRunningJob(jobSpec, job)
            }
        }
    }

    private fun timeUntilNextExecution(cronExpression: String): Duration {
        val cronParser = CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ))
        val cron: Cron = cronParser.parse(cronExpression)
        val now = ZonedDateTime.now(ZoneId.systemDefault())
        val executionTime = ExecutionTime.forCron(cron).nextExecution(now).getOrNull()
        return Duration.between(now, executionTime)
    }


    private suspend fun executeJob(jobSpec: JobSpec) {
        while (running) {
            val millisTilNextExecution = timeUntilNextExecution(jobSpec.schedule).toMillis()
            delay(millisTilNextExecution)
            val triggerEvents = ruleStateEngine.checkRules(jobSpec.customer, jobSpec.rules)
            when (jobSpec) {
                is EmailJobSpec -> executeEmailJob(jobSpec, triggerEvents)
                // add more job types here
            }
        }
    }

    private suspend fun executeEmailJob(jobSpec: EmailJobSpec, triggerEvents: List<TriggerEvent>) {
        val email = emailGenerator.generateEmail(jobSpec, triggerEvents)
        emailSender.sendEmail(email)
    }

    private data class JobSpecAndRunningJob(val jobSpec: JobSpec, val runningJob: Job)

}


data class DataSourceJobSpec(
    val name: String,
    val url: String,
    val username: String,
    val password: String,
    val model: String,
    val view: String,
    val type: String
)

data class RuleJobSpec(
    val name: String,
    val description: String,
    val datasource: DataSourceJobSpec,
    val filters: Map<String, String>,
    val fields: List<String>,
    val keys: Set<String>,
    val triggerOnNew: Boolean,
    val triggerOnChanged: Boolean,
    val triggerOnRemoved: Boolean
)

data class EmailJobSpec(
    override val name: String,
    override val customer: String,
    val email: String,
    val prompt: String,
    override val schedule: String,
    override val rules: List<RuleJobSpec>,
    override val id: Int
) : JobSpec

sealed interface JobSpec {
    val name: String
    val customer: String
    val schedule: String
    val rules: List<RuleJobSpec>
    val id: Int
}

private fun EmailTrigger.toJobSpec() = EmailJobSpec(
    email = email,
    prompt = prompt,
    schedule = trigger.schedule,
    rules = trigger.rules.map { it.toRuleJobSpec() },
    id = trigger.id.value,
    name = trigger.name,
    customer = trigger.owner.name
) as JobSpec

private fun Rule.toRuleJobSpec() = RuleJobSpec(
    name = name,
    description = description,
    datasource = datasource.toDataSourceJobSpec(),
    filters = filters,
    fields = properties,
    keys = keys.toSet(),
    triggerOnNew = triggerOnNew,
    triggerOnChanged = triggerOnChanged,
    triggerOnRemoved = triggerOnRemoved
)

private fun DataSource.toDataSourceJobSpec() = DataSourceJobSpec(
    name = name,
    url = url,
    username = username,
    password = password,
    model = model,
    view = view,
    type = DataSourceType.entries[type].name
)