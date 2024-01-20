val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val exposed_version: String by project
val h2_version: String by project
val google_http_version: String by project
val openai_client_version: String by project

plugins {
    kotlin("jvm") version "1.9.22"
    id("io.ktor.plugin") version "2.3.7"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22"
}

group = "com.edkohlwey"
version = "0.0.1"

application {
    mainClass.set("com.edkohlwey.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-resources")
    implementation("io.ktor:ktor-server-content-negotiation-jvm")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm")
    implementation("org.jetbrains.exposed:exposed-core:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-json:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-dao:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposed_version")
    implementation("com.aallam.openai:openai-client:$openai_client_version")

    implementation("com.h2database:h2:$h2_version")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("com.cronutils:cron-utils:9.1.3")
    implementation("com.hubspot.jinjava:jinjava:2.7.1")
    testImplementation("io.ktor:ktor-server-tests-jvm")
    testImplementation("io.ktor:ktor-client-content-negotiation-jvm")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
    // Looker
    implementation(files("lib/looker-kotlin-sdk.jar"))
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlin_version")

    implementation("io.github.cdimascio:dotenv-kotlin:6.2.2")
    implementation("org.ini4j:ini4j:0.5.4")

    implementation("commons-configuration:commons-configuration:1.10")

    implementation(platform("com.google.http-client:google-http-client-bom:$google_http_version"))
    implementation("com.google.http-client:google-http-client")
    implementation("com.google.http-client:google-http-client-apache-v2")
    implementation("com.google.http-client:google-http-client-gson")
    implementation("com.google.code.gson:gson:2.8.5")
    // End Looker

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
}
