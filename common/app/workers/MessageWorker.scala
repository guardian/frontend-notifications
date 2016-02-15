package workers

import javax.inject.{Inject, Singleton}

import com.amazonaws.auth.{AWSCredentialsProvider, DefaultAWSCredentialsProviderChain}
import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.sqs.AmazonSQSAsyncClient
import config.Config
import play.api.libs.json.Json
import services._

import scala.concurrent.Future

object PublishedMessage {
  implicit val implicitFormat = Json.format[PublishedMessage]
}

case class PublishedMessage(topic: String)

@Singleton
class MessageWorker @Inject() (
  config: Config,
  gcmWorker: GCMWorker,
  messageDatabase: MessageDatabase,
  clientDatabase: ClientDatabase) extends JsonQueueWorker[PublishedMessage] with Logging {
  import scala.concurrent.ExecutionContext.Implicits.global

  override val queue: JsonMessageQueue[PublishedMessage] =
    JsonMessageQueue[PublishedMessage](
      new AmazonSQSAsyncClient(
        new DefaultAWSCredentialsProviderChain()).withRegion(Region.getRegion(Regions.EU_WEST_1)),
      config.messageWorkerQueue)

  override def process(message: SQSMessage[PublishedMessage]): Future[Unit] = {
    val PublishedMessage(topic: String) = message.get

    log.info(s"Processing job for topic $topic")
    println(s"Processing job for topic $topic")

    clientDatabase.getIdsByTopic(topic).foreach{ browserId =>
      val gcmMessage = GCMMessage(s"Message for $topic", browserId.get, s"You got a new notification for $topic")
      messageDatabase.leaveMessage(gcmMessage)
      gcmWorker.queue.send(gcmMessage)
      //println(s"Notified $browserId about $topic")
      println(s"Got $browserId for $topic")
    }

    Future.successful(())
  }
}
