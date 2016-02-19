package config

import javax.inject.Inject

import com.amazonaws.regions.{Regions, Region}

import scala.util.Try

class Config @Inject()(config: play.Configuration) {
  val gcmSendRetries: Int = 2

  val NotificationMessagesTableName: String = "frontend-notification-messages"
  val ClientDatabaseTableName: String = "frontend-notifications"

  val firehoseRegionName: String = Regions.EU_WEST_1.getName
  val workerQueueRegion: Region = Region.getRegion(Regions.EU_WEST_1)

  val gcmKey: String = getMandatoryProperty("gcmKey")
  val gcmSendQueueUrl: String = getMandatoryProperty("gcmSendQueueUrl")

  val firehoseRole: String = getMandatoryProperty("firehoseRole")
  val firehoseStreamName: String = getMandatoryProperty("firehoseStreamName")

  val messageWorkerQueue: String = getMandatoryProperty("messageWorkerQueue")

  val redisMessageCacheHost: String = getMandatoryProperty("redisMessageCacheHost")
  val redisMessageCachePort: Int =
    Try(getMandatoryProperty("redisMessageCachePort"))
      .map(_.toInt)
      .getOrElse(throw new RuntimeException("Can't convert redisMessageCachePort to an integer"))

  def getMandatoryProperty(propertyName: String): String =
    Option(config.getString(propertyName))
      .getOrElse(throw new RuntimeException(s"Required property '$propertyName' not set"))
}
