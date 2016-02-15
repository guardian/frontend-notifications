package services

import javax.inject.Singleton

import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClient
import com.amazonaws.services.dynamodbv2.model.{ScanRequest, AttributeValue, QueryRequest}

import scala.collection.JavaConverters._

case class BrowserId(get: String)

@Singleton
class ClientDatabase {

  val dynamoDBClient: AmazonDynamoDBAsyncClient = new AmazonDynamoDBAsyncClient().withRegion(Region.getRegion(Regions.EU_WEST_1))
  val TableName: String = "frontend-notifications"

  def getIdsByTopic(topic: String): List[BrowserId] = {
    val queryRequest =
      new QueryRequest()
        .withTableName(TableName)
        .withKeyConditionExpression(s"notificationTopicId = :topic")
        .withExpressionAttributeValues(
          Map(":topic" -> new AttributeValue().withS(topic)).asJava)

    dynamoDBClient.query(queryRequest).getItems.asScala.flatMap { item =>
      item.asScala.get("gcmBrowserId").map(_.getS).map(BrowserId)
    }.toList
  }

}