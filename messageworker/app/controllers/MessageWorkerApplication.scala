package controllers

import javax.inject.{Inject, Singleton}

import play.api.mvc.{Action, Controller}
import services._
import workers._

import scala.concurrent.Future

@Singleton
class MessageWorkerApplication @Inject() (
  gcmWorkerModule: GCMWorkerModule,
  gcm: GCM,
  gCMWorker: GCMWorker,
  messageWorker: MessageWorkerModule,
  redisMessageDatabaseModule: RedisMessageDatabaseModule) extends Controller {

  val redisMessageDatabase: RedisMessageDatabase = redisMessageDatabaseModule.redisMessageDatabase

  import scala.concurrent.ExecutionContext.Implicits.global

  def index = Action { 
    Ok("Index OK from Application")
  }

  def info = Action {
    Ok(s"SQS Records Processed: ${ServerStatistics.recordsProcessed.get()}\n" +
      s"GCM Messages Sent: ${ServerStatistics.gcmMessagesSent.get()}")
  }

  def sendTo(browserId: String) = Action.async {
    gcm.sendSingle(GCMNotification("Test title", "message"), browserId)
      .map{ result =>
        Ok("Sent")}
      .recover{case t => InternalServerError(s"Error: $t") }
  }

  def send = Action {
    Ok(views.html.pushToOneSubscriber())
  }

  def sendToOne = Action.async { implicit request =>
    val requestMap: Map[String, Seq[String]] = request.body.asFormUrlEncoded.getOrElse(Map.empty)

    val maybeGCMMessage: Option[GCMMessage] = for {
      browserId <- requestMap.get("browserId").map(_.mkString)
      title <- requestMap.get("title").map(_.mkString)
      body <- requestMap.get("body").map(_.mkString)
      topicId <- requestMap.get("topicId").map(_.mkString)
    } yield GCMMessage(browserId, topicId, title, body)

    maybeGCMMessage match {
      case None => Future.successful(InternalServerError(s"Invalid parameters for $requestMap"))
      case Some(gcmMessage) =>
        (for {
          putItemResult <- redisMessageDatabase.leaveMessageWithDefaultExpiry(gcmMessage)
          sendMessageResult <- gCMWorker.queue.send(List(gcmMessage))}
         yield {
          Ok(s"Message sent: ${sendMessageResult.getMessageId}")})
        .recover { case t => InternalServerError(t.toString)}}
  }
}
