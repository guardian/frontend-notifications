package controllers

import javax.inject.{Inject, Singleton}

import play.api.mvc.{Action, Controller}
import services.{GCMNotification, GCM}
import workers.{GCMMessage, GCMWorker, GCMWorkerModule}

import scala.concurrent.Future
import scala.util.{Failure, Success}

@Singleton
class Application @Inject() (gcmWorker: GCMWorkerModule) extends Controller {

  import scala.concurrent.ExecutionContext.Implicits.global

  def index = Action { 
    Ok("Index OK from Application")
  }

  def sendTo(browserId: String) = Action.async {
    GCM.sendGcmNotification(GCMNotification("Test title", "message"), browserId)
      .map{ result =>
        result.getErrorCodeName
        Ok("Sent")}
      .recover{case t => InternalServerError(s"Error: $t") }
  }

  def send = Action {
    Ok(views.html.pushToOneSubscriber())
  }

  def sendToOne = Action.async { implicit request =>
    val requestMap: Map[String, Seq[String]] = request.body.asFormUrlEncoded.getOrElse(Map.empty)

    val maybeGCMMessage: Option[GCMMessage] = for {
      topic <- requestMap.get("topic").map(_.mkString)
      browserId <- requestMap.get("browserId").map(_.mkString)
    } yield GCMMessage(topic, browserId)

    maybeGCMMessage match {
      case None => Future.successful(InternalServerError(s"Invalid parameters for $requestMap"))
      case Some(gcmMessage) =>
        GCMWorker.queue.send(gcmMessage).map { result =>
          Ok(s"Message sent: ${result.getMessageId}")}
        .recover { case t => InternalServerError(t.toString)}}
  }
}
