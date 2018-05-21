/*
 * Copyright 2018 phData Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.phdata.pulse.alertengine.notification

import javax.mail._
import java.util.{ Date, Properties }
import javax.mail.internet.{ InternetAddress, MimeMessage }
import com.typesafe.scalalogging.LazyLogging

class Mailer(smtpServer: String,
             port: Long = 25,
             username: String,
             password: Option[String],
             use_smtp_tls: Boolean)
    extends LazyLogging {
  private val props = new Properties()
  props.put("mail.smtp.host", smtpServer)
  props.put("mail.smtp.port", port.toString)
  if (use_smtp_tls) {
    props.put("mail.smtp.starttls.enable", "true")
  }

  val session = password.fold {
    logger.info("no password supplied, skipping authentication")
    props.put("mail.smtp.auth", "false")
    Session.getInstance(props)
  } { password =>
    logger.info("authenticating with password")
    val auth = new Authenticator {
      override def getPasswordAuthentication = new PasswordAuthentication(username, password)
    }
    props.put("mail.smtp.auth", "true")
    Session.getInstance(props, auth)
  }

  def sendMail(addresses: List[String], subject: String, body: String): Unit = {
    val message: Message = new MimeMessage(session)
    message.setFrom(new InternetAddress(username))
    message.setSentDate(new Date())
    addresses.foreach { a =>
      message.addRecipients(Message.RecipientType.TO,
                            InternetAddress.parse(a).asInstanceOf[Array[Address]])
    }
    message.setSubject(subject)
    message.setText(body)
    Transport.send(message)
  }
}
