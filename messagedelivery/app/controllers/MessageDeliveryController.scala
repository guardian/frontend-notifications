package controllers

import javax.inject.{Inject, Singleton}

import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.{JsArray, JsObject, JsString, Json}
import play.api.mvc.{Action, Controller, Result}
import services.{Logging, RedisMessageDatabase}
import workers.GCMMessage

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


@Singleton
class MessageDeliveryController @Inject()(redis: RedisMessageDatabase) extends Controller with Logging {

  val gcmMessageForm = Form(
    mapping(
      "clientId" -> nonEmptyText,
      "topic" -> nonEmptyText,
      "title" -> nonEmptyText,
      "body" -> nonEmptyText,
      "blockId" -> nonEmptyText
    )(GCMMessage.apply)(GCMMessage.unapply))

  def index = Action {
    Ok("Index OK from Message Delivery")
  }

  val headerValues: Seq[(String, String)] = Seq("Access-Control-Allow-Origin" -> "*",
    "Access-Control-Allow-Headers" -> "Accept, Content-Type, Origin, Authorization",
    "Access-Control-Allow-Credentials" -> "true",
    "Access-Control-Allow-Max-Age" -> "3600",
    "Access-Control-Allow-Methods" -> "GET, POST")

  private def withCors(response: Result): Result = response.withHeaders(headerValues:_*)

  def getMessageOptions(browserId: String) = Action { implicit request =>
      NoContent.withHeaders(headerValues:_*)}

  def getMessage(browserId: String) = Action.async { implicit request =>
    redis.getMessages(browserId).map {
      case Nil =>
        log.warn(s"Could not retrieve latest message for $browserId")
        withCors(NotFound(JsObject(Seq("status" -> JsString("not found")))))
      case messages =>
        withCors(
          Ok(JsObject(
            Seq("status" -> JsString("ok"),
                "messages" -> JsArray(messages.map{ message => Json.toJson(message)})))))}}

  def saveRedisMessage() = Action.async { implicit request =>
    gcmMessageForm.bindFromRequest.fold(
      errors => Future.successful(BadRequest),
      gcmMessage =>
        redis.leaveMessageWithDefaultExpiry(gcmMessage).map(_ => Ok))}

  def acmeChallenge() = Action {
    Ok("fymWxrZRYXDKiDQouwLGgICaisvEXPYyQL2qBR2-tbE.IJbxXz31kpvuH8KkeakzDFx6mNFUDCDY9SNC38XNA50")
  }

}
