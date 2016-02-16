package config

import javax.inject.Inject

import com.amazonaws.regions.{Regions, Region}

class Config @Inject()(config: play.Configuration) {
  val gcmSendRetries: Int = 2

  val NotificationMessagesTableName: String = "frontend-notification-messages"

  val firehoseRegionName: String = Regions.EU_WEST_1.getName
  val workerQueueRegion: Region = Region.getRegion(Regions.EU_WEST_1)

  val gcmKey: String = getMandatoryProperty("gcmKey")
  val gcmSendQueueUrl: String = getMandatoryProperty("gcmSendQueueUrl")

  val firehoseRole: String = getMandatoryProperty("firehoseRole")
  val firehoseStreamName: String = getMandatoryProperty("firehoseStreamName")

  val messageWorkerQueue: String = getMandatoryProperty("messageWorkerQueue")

  def getMandatoryProperty(propertyName: String): String =
    Option(config.getString(propertyName))
      .getOrElse(s"Required property '$propertyName' not set")
}
