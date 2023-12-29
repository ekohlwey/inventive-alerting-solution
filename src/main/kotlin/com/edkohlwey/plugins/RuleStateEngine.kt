package com.edkohlwey.plugins

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime


interface RuleStateEngine {
    fun checkRules(customer: String, rules: List<RuleJobSpec>): List<TriggerEvent>
}


interface DataSourceConnection {
    fun checkForData(
        model: String,
        view: String,
        filters: Map<String, String>,
        fields: List<String>
    ): List<Map<String, String>>
}

interface DataSourceConnectionFactory {
    fun createDataSource(datasource: DataSourceJobSpec): DataSourceConnection
}

class DefaultDataSourceConnectionFactory : DataSourceConnectionFactory {
    override fun createDataSource(datasource: DataSourceJobSpec): DataSourceConnection {
        return when (datasource.type) {
            DataSourceType.LOOKER.name -> with(datasource) { LookerDataSourceConnection(url, username, password) }
            else -> throw IllegalArgumentException("Unsupported datasource type: ${datasource.type}")
        }
    }

}

class DatabaseRuleStateEngine(
    val database: Database,
    val connectionFactory: DataSourceConnectionFactory = DefaultDataSourceConnectionFactory()
) :
    RuleStateEngine {
    override fun checkRules(customer: String, rules: List<RuleJobSpec>): List<TriggerEvent> {
        val ruleJobSpecsByName = rules.associateBy { it.name }
        val currentRuleStatuses = rules.flatMap { it.getCurrentRuleStatuses(customer) }.associate { it }
        val ruleNames = rules.map { it.name }
        val previousRuleStatuses = transaction(database) {
            val oldRuleStates =
                Customers.innerJoin(Rules).innerJoin(RuleStates).slice(RuleStates.columns + Customers.name + Rules.name)
                    .select {
                        Customers.name eq customer and (Rules.name inList ruleNames)
                    }.map { RuleState.wrapRow(it) }
            return@transaction oldRuleStates.map { it.toRuleStatus() }.associate { it }
        }
        val newRuleStatuses =
            currentRuleStatuses.filter { it.isNewAndShouldTrigger(previousRuleStatuses, ruleJobSpecsByName) }
        val removedRuleStatuses =
            previousRuleStatuses.filter { it.isRemovedAndShouldTrigger(currentRuleStatuses, ruleJobSpecsByName) }
        val changedRuleStatuses =
            currentRuleStatuses.filter { it.isChangedAndShouldTrigger(previousRuleStatuses, ruleJobSpecsByName) }
                .filter { it.value != previousRuleStatuses[it.key] }

        val newEvents = newRuleStatuses.map { it.toTriggerEvent(TriggerKind.NEW, null) }
        val removedEvents =
            removedRuleStatuses.map { it.toTriggerEvent(TriggerKind.REMOVED, previousRuleStatuses, isRemoved = true) }
        val changedEvents = changedRuleStatuses.map { it.toTriggerEvent(TriggerKind.CHANGED, previousRuleStatuses) }
        return newEvents + removedEvents + changedEvents
    }

    private fun Map.Entry<RuleStatusKey, RuleStatus>.toTriggerEvent(
        triggerKind: TriggerKind,
        previousRuleStatuses: Map<RuleStatusKey, RuleStatus>?,
        isRemoved: Boolean = false
    ): TriggerEvent {
        return TriggerEvent(
            kind = triggerKind,
            customer = key.customer,
            rule = key.rule,
            currentValues = if (!isRemoved) value.allValues else null,
            oldValues = previousRuleStatuses?.get(key)?.allValues
        )
    }

    private fun Map.Entry<RuleStatusKey, RuleStatus>.isChangedAndShouldTrigger(
        previousRuleStatuses: Map<RuleStatusKey, RuleStatus>,
        ruleJobSpecsByName: Map<String, RuleJobSpec>
    ) = this.key in previousRuleStatuses && ruleJobSpecsByName[this.key.rule]?.triggerOnChanged ?: false

    private fun Map.Entry<RuleStatusKey, RuleStatus>.isRemovedAndShouldTrigger(
        currentRuleStatuses: Map<RuleStatusKey, RuleStatus>,
        ruleJobSpecsByName: Map<String, RuleJobSpec>
    ) = this.key !in currentRuleStatuses && ruleJobSpecsByName[this.key.rule]?.triggerOnRemoved ?: false

    private fun Map.Entry<RuleStatusKey, RuleStatus>.isNewAndShouldTrigger(
        previousRuleStatuses: Map<RuleStatusKey, RuleStatus>,
        ruleJobSpecsByName: Map<String, RuleJobSpec>
    ) = this.key !in previousRuleStatuses && ruleJobSpecsByName[this.key.rule]?.triggerOnNew ?: false


    private fun RuleState.toRuleStatus(): Pair<RuleStatusKey, RuleStatus> {
        return RuleStatusKey(
            customer = customer.name,
            rule = rule.name,
            keyValues = keyValues,
        ) to RuleStatus(
            allValues = allValues,
            timeStamp = lastUpdated
        )
    }

    data class RuleStatusKey(
        val rule: String, val customer: String, val keyValues: Map<String, String>
    )

    data class RuleStatus(
        val allValues: Map<String, String>, val timeStamp: LocalDateTime
    )

    private fun RuleJobSpec.getCurrentRuleStatuses(customer: String): List<Pair<RuleStatusKey, RuleStatus>> {
        val results = connectionFactory.createDataSource(datasource).checkForData(
            model = datasource.model,
            view = datasource.view,
            filters = filters,
            fields = fields
        )
        val currentTime = LocalDateTime.now()
        return results.map { result ->
            val keyValues = result.filterKeys { keys.contains(it) }
            RuleStatusKey(
                customer = customer,
                rule = name,
                keyValues = keyValues,
            ) to RuleStatus(
                allValues = result, timeStamp = currentTime
            )
        }
    }
}

enum class TriggerKind {
    NEW, CHANGED, REMOVED
}

data class TriggerEvent(
    val kind: TriggerKind,
    val customer: String,
    val rule: String,
    val currentValues: Map<String, String>? = null,
    val oldValues: Map<String, String>? = null
)