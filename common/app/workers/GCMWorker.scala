package workers

import javax.inject.{Inject, Singleton}

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.sqs.AmazonSQSAsyncClient
import config.Config
import play.api.libs.json.Json
import services._

import scala.concurrent.Future
import scala.util.{Failure, Success}

object GCMMessage {
  implicit val implicitFormat = Json.format[GCMMessage]
}

case class GCMMessage(topic: String, clientId: String, body: String)

@Singleton
class GCMWorker @Inject()(
  config: Config,
  gcm: GCM) extends JsonQueueWorker[GCMMessage] with Logging {
  import scala.concurrent.ExecutionContext.Implicits.global

  override val queue: JsonMessageQueue[GCMMessage] =
    JsonMessageQueue[GCMMessage](
      new AmazonSQSAsyncClient(
        new DefaultAWSCredentialsProviderChain()).withRegion(Region.getRegion(Regions.EU_WEST_1)),
      config.gcmSendQueueUrl)

  override def process(message: SQSMessage[GCMMessage]): Future[Unit] = {
    val GCMMessage(topic: String, clientId: String, body: String) = message.get

    log.info(s"Processing job for topic $topic to $clientId")

    val futureResult = gcm.sendGcmNotification(GCMNotification("title", "body"), clientId)

    futureResult.onComplete {
      case Success(result) => log.info(s"Successfully sent notification to $clientId: ${message.handle.get}")
      case Failure(t) => log.error(s"Error sending notification to $clientId: $t")
    }

    futureResult.map(_ => ())
  }
}