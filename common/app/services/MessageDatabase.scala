package services

import javax.inject.Singleton

import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClient
import com.amazonaws.services.dynamodbv2.model.{AttributeValue, PutItemRequest, PutItemResult}
import workers.GCMMessage

import scala.collection.JavaConverters._

case class LeaveMessageResult(get: String)

@Singleton
class MessageDatabase {

  val dynamoDBClient: AmazonDynamoDBAsyncClient = new AmazonDynamoDBAsyncClient().withRegion(Region.getRegion(Regions.EU_WEST_1))
  val TableName = "latest-notification-test"

  def leaveMessage(gcmMessage: GCMMessage): PutItemResult = {
    val putItemRequest =
      new PutItemRequest()
          .withTableName(TableName)
          .withItem(Map(
            "gcmBrowserId" -> new AttributeValue().withS(gcmMessage.clientId),
            "messages" -> new AttributeValue().withL(
              new AttributeValue().withM(
                Map(
                  "topic" -> new AttributeValue().withS(gcmMessage.topic),
                  "body" -> new AttributeValue().withS(gcmMessage.body)
                ).asJava))
          ).asJava)

    dynamoDBClient.putItem(putItemRequest)
  }
}
