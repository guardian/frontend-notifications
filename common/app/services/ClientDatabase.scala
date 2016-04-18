package services

import javax.inject.{Inject, Singleton}

import awswrappers.dynamodb._
import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClient
import com.amazonaws.services.dynamodbv2.model.{AttributeValue, QueryRequest}
import config.Config
import helper.{GcmId, BrowserEndpoint}

import scala.collection.JavaConverters._
import scala.concurrent.Future

@Singleton
class ClientDatabase @Inject()(
  config: Config) {

  import scala.concurrent.ExecutionContext.Implicits.global

  val dynamoDBClient: AmazonDynamoDBAsyncClient = new AmazonDynamoDBAsyncClient().withRegion(Region.getRegion(Regions.EU_WEST_1))
  val TableName: String = config.ClientDatabaseTableName

  def getIdsByTopic(topic: String): Future[List[BrowserEndpoint]] = {

    def getQueryRequest(maybeLastEvaluatedKey: Option[java.util.Map[String, AttributeValue]], results: List[BrowserEndpoint]): Future[List[BrowserEndpoint]] = {
      val queryRequest: QueryRequest =
        new QueryRequest()
          .withTableName(TableName)
          .withLimit(1000)
          .withKeyConditionExpression(s"notificationTopicId = :topic")
          .withExpressionAttributeValues(
            Map(":topic" -> new AttributeValue().withS(topic)).asJava)

      val finalQueryRequest: QueryRequest = maybeLastEvaluatedKey
        .fold(queryRequest)(lastEvaluatedKey => queryRequest.withExclusiveStartKey(lastEvaluatedKey))

      dynamoDBClient.queryFuture(finalQueryRequest).flatMap { queryResult =>
        val newResults = results ::: queryResult.getItems.asScala.flatMap { item =>
          val itemMap: scala.collection.mutable.Map[String, AttributeValue] = item.asScala

            itemMap.get("browserEndpoint")
              .map(_.getS)
              .flatMap(BrowserEndpoint.fromEndpointUrl)
        }.toList

        Option(queryResult.getLastEvaluatedKey) match {
          case Some(lastEvaluatedKey) if !queryResult.getLastEvaluatedKey.isEmpty => getQueryRequest(Option(lastEvaluatedKey), newResults)
          case None => Future.successful(newResults)}}}


      getQueryRequest(None, Nil)
    }
}