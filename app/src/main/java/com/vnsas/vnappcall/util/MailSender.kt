package com.vnsas.vnappcall.util

import com.vnsas.vnappcall.data.MailSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Properties
import javax.activation.DataHandler
import javax.activation.FileDataSource
import javax.mail.Message
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

object MailSender {

    suspend fun sendTestEmail(settings: MailSettings): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val session = createSession(settings)
            val msg = MimeMessage(session).apply {
                setFrom(InternetAddress(settings.from))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(settings.to))
                subject = "Test mail da VNAppCall"
                setText("Questa è una mail di test inviata dall'app VNAppCall.")
            }
            Transport.send(msg)
        }
    }

    suspend fun sendReportEmail(
        settings: MailSettings,
        subject: String,
        body: String,
        attachmentFile: File?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val session = createSession(settings)
            val msg = MimeMessage(session).apply {
                setFrom(InternetAddress(settings.from))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(settings.to))
                this.subject = subject
            }

            if (attachmentFile != null) {
                val multipart = MimeMultipart()
                val textPart = MimeBodyPart().apply { setText(body) }
                multipart.addBodyPart(textPart)

                val filePart = MimeBodyPart().apply {
                    dataHandler = DataHandler(FileDataSource(attachmentFile))
                    fileName = attachmentFile.name
                }
                multipart.addBodyPart(filePart)
                msg.setContent(multipart)
            } else {
                msg.setText(body)
            }

            Transport.send(msg)
        }
    }

    private fun createSession(settings: MailSettings): Session {
        val props = Properties().apply {
            put("mail.smtp.host", settings.host)
            put("mail.smtp.port", settings.port)
            put("mail.smtp.auth", "true")
            if (settings.ssl) {
                put("mail.smtp.starttls.enable", "true")
                put("mail.smtp.ssl.trust", settings.host)
            }
            put("mail.smtp.connectiontimeout", "15000")
            put("mail.smtp.timeout", "15000")
        }
        return Session.getInstance(props, object : javax.mail.Authenticator() {
            override fun getPasswordAuthentication() =
                javax.mail.PasswordAuthentication(settings.user, settings.pass)
        })
    }
}
