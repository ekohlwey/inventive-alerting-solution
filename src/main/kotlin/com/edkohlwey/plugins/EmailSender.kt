package com.edkohlwey.plugins

import io.ktor.util.logging.*

interface EmailSender {
    fun sendEmail(email: String)
}

private val LOGGER = KtorSimpleLogger(EmailSender::class.qualifiedName!!)

class LogEmailSender : EmailSender {
    override fun sendEmail(email: String) {
        LOGGER.info("Sending email: $email")
    }
}