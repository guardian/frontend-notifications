package services

import javax.inject.{Inject, Singleton}

import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClient
import com.amazonaws.services.dynamodbv2.model.{AttributeValue, QueryRequest}
import config.Config

import scala.collection.JavaConverters._

case class BrowserId(get: String)

@Singleton
class ClientDatabase @Inject()(
  config: Config) {

  val dynamoDBClient: AmazonDynamoDBAsyncClient = new AmazonDynamoDBAsyncClient().withRegion(Region.getRegion(Regions.EU_WEST_1))
  val TableName: String = config.ClientDatabaseTableName

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