package workers

import javax.inject.{Inject, Singleton}

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.services.sqs.AmazonSQSAsyncClient
import config.Config
import play.api.libs.json.Json
import services._

import scala.concurrent.Future

object PublishedMessage {
  implicit val implicitFormat = Json.format[PublishedMessage]
}

case class PublishedMessage(topic: String) extends AnyVal

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
        new DefaultAWSCredentialsProviderChain()).withRegion(config.workerQueueRegion),
      config.messageWorkerQueue)

  override def process(message: SQSMessage[PublishedMessage]): Future[Unit] = {
    val PublishedMessage(topic: String) = message.get

    log.info(s"Processing job for topic $topic")

    clientDatabase.getIdsByTopic(topic).foreach{ browserId =>
      val gcmMessage: GCMMessage = GCMMessage(browserId.get, topic, s"Message for $topic", s"You got a new notification for $topic")
      messageDatabase.leaveMessage(gcmMessage)
      gcmWorker.queue.send(gcmMessage)
    }

    Future.successful(())
  }
}
