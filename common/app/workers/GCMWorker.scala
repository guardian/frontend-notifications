package workers

import javax.inject.{Inject, Singleton}

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.services.sqs.AmazonSQSAsyncClient
import com.google.android.gcm.server.MulticastResult
import config.Config
import play.api.libs.json.Json
import services._

import scala.concurrent.Future
import scala.util.{Failure, Success}

object GCMMessage {
  implicit val implicitFormat = Json.format[GCMMessage]
}

case class GCMMessage(clientId: String, topic: String, title: String, body: String)

@Singleton
class GCMWorker @Inject()(
  config: Config,
  gcm: GCM) extends JsonQueueWorker[List[GCMMessage]] with Logging {
  import scala.concurrent.ExecutionContext.Implicits.global

  override val queue: JsonMessageQueue[List[GCMMessage]] =
    JsonMessageQueue[List[GCMMessage]](
      new AmazonSQSAsyncClient(
        new DefaultAWSCredentialsProviderChain()).withRegion(config.workerQueueRegion),
      config.gcmSendQueueUrl)

  override def process(message: SQSMessage[List[GCMMessage]]): Future[Unit] = {
    val listOfMessages: List[GCMMessage] = message.get

    val browserIds: List[BrowserId] = listOfMessages
      .map(_.clientId)
      .map(BrowserId)

    log.info(s"Processing job for ${listOfMessages.size} clients")
    log.info(s"Processing job for $browserIds")

    val futureResult: Future[MulticastResult] = gcm.sendMulticast(None, browserIds)

    futureResult.onComplete {
      case Success(multicastResult) =>
        log.info(s"Multicast Result $multicastResult")
        log.info(s"Successfully sent notification to ${multicastResult.getSuccess}: ${message.handle.get}")
        if (multicastResult.getFailure > 0) {
          log.error(s"Error sending notifications to ${multicastResult.getFailure}")}
      case Failure(t) => log.error(s"Error sending notification to ${listOfMessages.length} clients: $t")
    }

    futureResult.map(_ => ())
  }
}