package helper

import awswrappers.dynamodb._
import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClient
import com.amazonaws.services.dynamodbv2.model._
import helper.DynamoFormat.DynamoFormat

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object DynamoFormat {
  trait DynamoFormat[T] {
    def read(value: AttributeValue): Option[T]
    def write(value: T): AttributeValue
  }
}

class DynamoLockingUpdateTable[T](
  dynamoDBClient: AmazonDynamoDBAsyncClient,
  tableName: String,
  primaryKeyName: String,
  dataKeyName: String)(
  implicit dynamoFormat: DynamoFormat[T]) {

  sealed trait UpdateResult
  case class NewItem(get: T) extends UpdateResult
  case class ConditionFailed(old: T) extends UpdateResult
  case class ReadAndWrite(old: T, newItem: T) extends UpdateResult
  object FailedConditionOnWrite extends UpdateResult

  def consistentRead(id: String): Future[Option[AttributeValue]] =
    dynamoDBClient.getItemFuture(
      new GetItemRequest()
        .withConsistentRead(true)
        .withTableName(tableName)
        .withKey(Map(primaryKeyName -> new AttributeValue().withS(id)).asJava))
      .map { getItemResult =>
        Option(getItemResult.getItem)
          .map(_.asScala)
          .getOrElse(Map.empty[String, AttributeValue])
          .get(dataKeyName)}

  private def consistentFormatRead(id: String): Future[Option[T]] =
    consistentRead(id).map(_.flatMap(dynamoFormat.read))

  //At this point, you've read it and know it exists
  private def conditionalWrite(id: String, old: T, a: T): Future[PutItemResult] =
    dynamoDBClient.putItemFuture(
      new PutItemRequest()
        .withTableName(tableName)
        .withItem(
          Map(
            primaryKeyName -> new AttributeValue().withS(id),
            dataKeyName -> dynamoFormat.write(a)).asJava)
        .withConditionExpression(s"$dataKeyName = :data")
        .withExpressionAttributeValues(
          Map(":data" -> dynamoFormat.write(old)).asJava))

  private def newItem(id: String, a: T): Future[PutItemResult] =
    dynamoDBClient.putItemFuture(
      new PutItemRequest()
        .withTableName(tableName)
        .withItem(
          Map(
            primaryKeyName -> new AttributeValue().withS(id),
            dataKeyName -> dynamoFormat.write(a)).asJava))

  def lockingReadAndWriteWithCondition(id: String, empty: T)
      (updateFunction: T => Option[T]): Future[UpdateResult] =
    consistentFormatRead(id).flatMap {
      case Some(value) =>
        updateFunction(value) match {
          case Some(v) =>
            conditionalWrite(id, value, v)
              .map(_ => ReadAndWrite(value, v))
              .recover{ case conditionFailedException: ConditionalCheckFailedException =>
                FailedConditionOnWrite}
          case None => Future.successful(ConditionFailed(value))
        }
      case None => newItem(id, empty).map(Function.const(NewItem(empty)))
    }

  def lockingReadAndWriteWithUpdate(id: String, empty: T)(updateFunction: T => T): Future[UpdateResult] =
    lockingReadAndWriteWithCondition(id, empty)(t => Option(updateFunction(t)))

  def lockingReadAndWriteIgnore(id: String, item: T): Future[UpdateResult] =
    lockingReadAndWriteWithUpdate(id, item)(Function.const(item))
}
