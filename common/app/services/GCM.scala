package services

import javax.inject.{Inject, Singleton}

import com.google.android.gcm.server.Message.Builder
import com.google.android.gcm.server.{Result, Message, Sender}
import config.Config

import scala.concurrent.Future

case class GCMNotification(title: String, message: String)

object GCMNotification {

  def toMessage(gcmNotification: GCMNotification): Message =
    new Builder()
      .build()

}

@Singleton
class GCM @Inject()(config: Config) {
  import scala.concurrent.ExecutionContext.Implicits.global

  val gcmClient: Sender = new Sender(config.gcm.apiKey)

  def sendGcmNotification(gcmNotification: GCMNotification, browserId: String): Future[Result] =
    Future.apply(gcmClient.send(GCMNotification.toMessage(gcmNotification), browserId, 2))

}
