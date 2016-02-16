package services

import javax.inject.{Inject, Singleton}

import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClient
import com.amazonaws.services.dynamodbv2.model.{AttributeValue, PutItemRequest, PutItemResult}
import config.Config
import workers.GCMMessage

import scala.collection.JavaConverters._

case class LeaveMessageResult(get: String)

@Singleton
class MessageDatabase @Inject()(
  config: Config) {

  val dynamoDBClient: AmazonDynamoDBAsyncClient = new AmazonDynamoDBAsyncClient().withRegion(Region.getRegion(Regions.EU_WEST_1))
  val TableName: String = config.NotificationMessagesTableName

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
                  "title" -> new AttributeValue().withS(gcmMessage.title),
                  "body" -> new AttributeValue().withS(gcmMessage.body)
                ).asJava))
          ).asJava)

    dynamoDBClient.putItem(putItemRequest)
  }
}
