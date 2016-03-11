package services

import javax.inject.Inject

import com.amazonaws.regions.{Regions, Region}
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClient
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.google.inject.Singleton
import config.Config
import helper.DynamoFormat.DynamoFormat
import helper.DynamoLockingUpdateTable
import org.joda.time.DateTime

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

sealed trait LastSent

case class LastSentDateOnly(topic: String, dateTime: DateTime) extends LastSent

object LastSentDateOnly {
  def emptyForTopic(topic: String): LastSent = LastSentDateOnly(topic, DateTime.now())
}

case class LastSentKeyEvent(topic: String, dateTime: DateTime, keyEventId: Option[String]) extends LastSent

object LastSent {
  implicit object LastSentDynamoFormat extends DynamoFormat[LastSent] {
    override def read(value: AttributeValue): Option[LastSent] = {
      val valueMap: Map[String, AttributeValue] =
        Option(value.getM).map(_.asScala.toMap)
          .getOrElse(Map.empty)

      valueMap.get("type").flatMap(av => Option(av.getS)) match {
        case Some("lastSentDateOnly") =>
          for {
            topic <- valueMap.get("topic").flatMap(av => Option(av.getS))
            dateTime <- valueMap.get("dateTime")
              .flatMap(av => Option(av.getN))
              .map(_.toLong)
              .map(l => new DateTime(l * 1000))
          } yield LastSentDateOnly(topic, dateTime)
        case Some("lastSentKeyEvent") =>
          for {
            topic <- valueMap.get("topic").flatMap(av => Option(av.getS))
            dateTime <- valueMap.get("dateTime")
              .flatMap(av => Option(av.getN))
              .map(_.toLong)
              .map(l => new DateTime(l * 1000))
            keyEvent = valueMap.get("keyEvent").flatMap(av => Option(av.getS))
          } yield LastSentKeyEvent(topic, dateTime, keyEvent)
        case _ => None
      }


    }

    override def write(value: LastSent): AttributeValue = {
      value match {
        case lastSentDateOnly: LastSentDateOnly =>
          new AttributeValue().withM(
            Map(
              "type" -> new AttributeValue().withS("lastSentDateOnly"),
              "topic" -> new AttributeValue().withS(lastSentDateOnly.topic),
              "dateTime" -> new AttributeValue().withN((lastSentDateOnly.dateTime.getMillis / 1000).toString)
            ).asJava)
        case lastSentKeyEvent: LastSentKeyEvent =>
          val attributeMap = Map(
            "type" -> new AttributeValue().withS("lastSentKeyEvent"),
            "topic" -> new AttributeValue().withS(lastSentKeyEvent.topic),
            "dateTime" -> new AttributeValue().withN((lastSentKeyEvent.dateTime.getMillis / 1000).toString))

          new AttributeValue().withM(
            lastSentKeyEvent.keyEventId.fold(attributeMap)(s => attributeMap + ("keyEvent" -> new AttributeValue().withS(s))).asJava)
      }
    }
  }
}