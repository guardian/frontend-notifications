package services

import javax.inject.{Inject, Singleton}

import com.google.android.gcm.server.Message.Builder
import com.google.android.gcm.server.{MulticastResult, Result, Message, Sender}
import config.Config

import scala.concurrent.Future
import scala.collection.JavaConverters._

case class GCMNotification(title: String, body: String)

object GCMNotification {

  def toMessage(gcmNotification: GCMNotification): Message =
    new Builder()
      .build()

}

@Singleton
class GCM @Inject()(config: Config) {
  import scala.concurrent.ExecutionContext.Implicits.global

  val gcmClient: Sender = new Sender(config.gcm.apiKey)

  def sendSingle(gcmNotification: GCMNotification, browserId: String): Future[Result] =
    Future.apply(gcmClient.send(GCMNotification.toMessage(gcmNotification), browserId, config.gcmSendRetries))

  def sendMulticast(gcmNotification: GCMNotification, listOfBrowserIds: List[BrowserId]): Future[MulticastResult] =
    Future.apply(gcmClient.send(GCMNotification.toMessage(gcmNotification), listOfBrowserIds.map(_.get).asJava, config.gcmSendRetries))

}
