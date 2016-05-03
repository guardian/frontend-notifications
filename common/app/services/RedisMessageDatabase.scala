package services

import javax.inject.{Inject, Singleton}

import akka.actor.ActorSystem
import akka.util.ByteString
import config.Config
import org.joda.time.DateTime
import play.api.inject.ApplicationLifecycle
import play.api.libs.json.Json
import redis.{ByteStringFormatter, RedisClient}
import redis.commands.TransactionBuilder
import redis.protocol.MultiBulk
import workers.GCMMessage

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

import scala.concurrent.ExecutionContext.Implicits.global

case class RedisMessage(topic: String, title: String, body: String, blockId: Option[String], time: Long)

object RedisMessage {
  implicit val redisMessageFormat = Json.format[RedisMessage]

  implicit val formatter = new ByteStringFormatter[RedisMessage] {
    override def serialize(redisMessage: RedisMessage): ByteString = {
      ByteString(Json.stringify(Json.toJson(redisMessage)))}

    override def deserialize(bs: ByteString): RedisMessage = {
      val redisMessage = Json.parse(bs.utf8String)
      redisMessage.as[RedisMessage]
    }
  }

  def fromGcmMessage(gcmMessage: GCMMessage): RedisMessage =
    RedisMessage(gcmMessage.topic, gcmMessage.title, gcmMessage.body, gcmMessage.blockId, DateTime.now.getMillis / 1000)
}

@Singleton
class RedisMessageDatabaseModule @Inject()(
  applicationLifecycle: ApplicationLifecycle,
  val redisMessageDatabase: RedisMessageDatabase) extends Logging {

  log.info("Starting redis message database actor system")

  applicationLifecycle.addStopHook{ () =>
    log.info("Shutting down redis message database actor system")
    Future.successful(redisMessageDatabase.akkaSystem.terminate())
  }
}


@Singleton
class RedisMessageDatabase @Inject()(
  config: Config) extends Logging {

  implicit lazy val akkaSystem: ActorSystem = akka.actor.ActorSystem()
  lazy val redis: RedisClient = RedisClient(host=config.redisMessageCacheHost, port=config.redisMessageCachePort)
  val defaultExpiryInSeconds: Int = 12.hours.toSeconds.toInt

  def pingRedis: Future[String] = redis.ping()

  def leaveMessageWithExpiry(gcmMessage: GCMMessage, expiry: Int): Future[MultiBulk] = {
    val redisTransaction: TransactionBuilder = redis.transaction()
    redisTransaction.lpush(gcmMessage.clientId, Json.stringify(Json.toJson(RedisMessage.fromGcmMessage(gcmMessage))))
    redisTransaction.expire(gcmMessage.clientId, expiry)
    val transactionResult: Future[MultiBulk] = redisTransaction.exec()

    transactionResult.onComplete {
      case Success(multiBulk) => log.info(s"Successful redis transaction for ${gcmMessage.clientId} ($multiBulk)")
      case Failure(t) => log.error(s"Error in redis transaction: $t")
    }

    transactionResult
  }

  def leaveMessageWithDefaultExpiry(gcmMessage: GCMMessage) = leaveMessageWithExpiry(gcmMessage, defaultExpiryInSeconds)

  def getMessages(gcmClientId: String) : Future[Seq[RedisMessage]] = {
    val redisTransaction: TransactionBuilder = redis.transaction()
    val eventualMessages: Future[Seq[RedisMessage]] = redisTransaction.lrange[RedisMessage](gcmClientId, 0, -1)
    redisTransaction.del(gcmClientId)
    redisTransaction.exec()
    eventualMessages
  }
}