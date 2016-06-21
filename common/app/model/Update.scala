package model

import com.gu.contentapi.client.model.v1.Content
import julienrf.json.derived
import play.api.libs.json.{Json, Format}
import services.Sentences

import scala.util.Try

case class KeyEvent(id: String, title: Option[String], body: String)

object KeyEvent {
  implicit val implicitFormat: Format[KeyEvent] = Json.format[KeyEvent]

  val keyEventBodySize: Int = 412

  def fromContent(content: Content): List[KeyEvent] =
    content.blocks
      .flatMap(_._2)
      .getOrElse(Nil)
      .filter(_.attributes.keyEvent.exists(identity))
      .filter(_.published)
      .map(block => KeyEvent(block.id, block.title, Sentences.reduceToWithEllipsis(block.bodyTextSummary, keyEventBodySize)))
      .toList
      .reverse

  def getLastestKeyEvents(lastKeyEventId: String, keyEvents: List[KeyEvent]): List[KeyEvent] =
    Try(keyEvents.dropWhile(_.id != lastKeyEventId).tail).toOption.getOrElse(Nil)
}

sealed trait Update
case class PublishedMessage(topic: String) extends Update
case class LiveBlogUpdateEvent(topic: String, keyEvents: List[KeyEvent]) extends Update

object Update {
  implicit val implicitFormat: Format[Update] = derived.oformat
}