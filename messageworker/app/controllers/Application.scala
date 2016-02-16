package controllers

import javax.inject.{Inject, Singleton}

import play.api.mvc.{Action, Controller}
import services.{GCM, GCMNotification, MessageDatabase}
import workers._

import scala.concurrent.Future

@Singleton
class Application @Inject() (
  gcmWorkerModule: GCMWorkerModule,
  gcm: GCM,
  gCMWorker: GCMWorker,
  messageWorker: MessageWorkerModule,
  messageDatabase: MessageDatabase) extends Controller {

  import scala.concurrent.ExecutionContext.Implicits.global

  def index = Action { 
    Ok("Index OK from Application")
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
    } yield GCMMessage(browserId, "notopic", title, body)

    maybeGCMMessage match {
      case None => Future.successful(InternalServerError(s"Invalid parameters for $requestMap"))
      case Some(gcmMessage) =>
        messageDatabase.leaveMessage(gcmMessage)
        gCMWorker.queue.send(List(gcmMessage)).map { result =>
          Ok(s"Message sent: ${result.getMessageId}")}
        .recover { case t => InternalServerError(t.toString)}}
  }
}
