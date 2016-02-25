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


case class LastSent(topic: String, dateTime: DateTime)

object LastSent {
  implicit object LastSentDynamoFormat extends DynamoFormat[LastSent] {
    override def read(value: AttributeValue): Option[LastSent] = {
      val valueMap: Map[String, AttributeValue] =
        Option(value.getM).map(_.asScala.toMap)
          .getOrElse(Map.empty)

      for {
        topic <- valueMap.get("topic").flatMap(av => Option(av.getS))
        dateTime <- valueMap.get("dateTime")
          .flatMap(av => Option(av.getN))
          .map(_.toLong)
          .map(l => new DateTime(l * 1000))
      } yield LastSent(topic, dateTime)
    }

    override def write(value: LastSent): AttributeValue = {
      new AttributeValue().withM(
        Map(
          "topic" -> new AttributeValue().withS(value.topic),
          "dateTime" -> new AttributeValue().withN((value.dateTime.getMillis / 1000).toString)
        ).asJava)
    }
  }
}

@Singleton
class LastSentDatabase @Inject()(config: Config) extends Logging {

  sealed trait ShouldSendResult
  case object ShouldSend extends ShouldSendResult
  case object ShouldNotSend extends ShouldSendResult

  val dynamoClient: AmazonDynamoDBAsyncClient = new AmazonDynamoDBAsyncClient().withRegion(Region.getRegion(Regions.EU_WEST_1))
  val lockingUpdate: DynamoLockingUpdateTable[LastSent] = new DynamoLockingUpdateTable[LastSent](
    dynamoClient,
    config.LastSentTableName,
    "topicId",
    "dataKey")

  def emptyForTopic(topic: String): LastSent = LastSent(topic, DateTime.now())

  def updateTopic(topic: String): Future[ShouldSendResult] = {
    val update = lockingUpdate.lockingReadAndWriteWithCondition(topic, emptyForTopic(topic)){
      case LastSent(t, dateTime) =>
        if (DateTime.now.minusMinutes(2).isAfter(dateTime))
          Option(LastSent(t, DateTime.now))
        else
          None}

    update.map {
      case lockingUpdate.ReadAndWrite(old, _) =>
        log.info(s"Sending to $topic; (Last sent: $old)")
        ShouldSend
      case _ => ShouldNotSend}
  }
}