package services

import javax.inject.{Inject, Singleton}

import akka.actor.ActorSystem
import config.Config
import org.joda.time.DateTime
import play.api.inject.ApplicationLifecycle
import play.api.libs.json.Json
import redis.RedisClient
import redis.commands.TransactionBuilder
import redis.protocol.MultiBulk
import workers.GCMMessage

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

import scala.concurrent.ExecutionContext.Implicits.global

case class RedisMessage(topic: String, title: String, body: String, time: Long)

object RedisMessage {
  implicit val redisMessageFormat = Json.format[RedisMessage]

  def fromGcmMessage(gcmMessage: GCMMessage): RedisMessage =
    RedisMessage(gcmMessage.topic, gcmMessage.title, gcmMessage.body, DateTime.now.getMillis / 1000)
}

@Singleton
class RedisMessageDatabaseModule @Inject()(
  applicationLifecycle: ApplicationLifecycle,
  val redisMessageDatabase: RedisMessageDatabase) extends Logging {

  log.info("Starting redis message database actor system")

  applicationLifecycle.addStopHook{ () =>
    log.info("Shutting down redis message database actor system")
    Future.successful(redisMessageDatabase.akkaSystem.shutdown())
  }
}


@Singleton
class RedisMessageDatabase @Inject()(
  config: Config) extends Logging {

  implicit val akkaSystem: ActorSystem = akka.actor.ActorSystem()
  val redis: RedisClient = RedisClient(host=config.redisMessageCacheHost, port=config.redisMessageCachePort)
  val defaultExpiryInSeconds: Int = 5.minutes.toSeconds.toInt

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
}