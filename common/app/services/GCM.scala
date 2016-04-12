package services

import javax.inject.{Inject, Singleton}

import com.google.android.gcm.server.Message.Builder
import com.google.android.gcm.server.{MulticastResult, Result, Message, Sender}
import config.Config
import helper.GcmId

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

  val gcmClient: Sender = new Sender(config.gcmKey)

  def sendSingle(gcmNotification: GCMNotification, browserId: String): Future[Result] =
    Future.apply(gcmClient.send(GCMNotification.toMessage(gcmNotification), browserId, config.gcmSendRetries))

  def sendMulticast(gcmNotification: Option[GCMNotification], listOfBrowserIds: List[GcmId]): Future[MulticastResult] = {
    val message: Message = gcmNotification match {
      case None => GCMNotification.toMessage(GCMNotification("", ""))
      case Some(n) => GCMNotification.toMessage(n)}

    Future.apply(gcmClient.send(message, listOfBrowserIds.map(_.get).asJava, config.gcmSendRetries))}

  def sendMulticastWithoutPayload(listOfBrowserIds: List[GcmId]): Future[MulticastResult] =
    sendMulticast(None, listOfBrowserIds)

}
