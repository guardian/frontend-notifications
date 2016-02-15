package config

import javax.inject.Inject

case class GCMCredentials(apiKey: String)

class Config @Inject()(config: play.Configuration) {
  val gcmSendRetries: Int = 2

  private val gcmKey: String =
    Option(config.getString("gcmKey"))
        .getOrElse(throw new RuntimeException("No gcmKey set"))

  val gcm: GCMCredentials =
    GCMCredentials(gcmKey)

  val gcmSendQueueUrl: String =
    Option(config.getString("gcmSendQueueUrl"))
      .getOrElse("Required property 'gcmSendQueueUrl' not set")

  val firehoseRole: String =
    Option(config.getString("firehoseRole"))
      .getOrElse(throw new RuntimeException("Property not set 'firehoseRole'"))

  val firehoseStreamName: String =
    Option(config.getString("firehoseStreamName"))
      .getOrElse(throw new RuntimeException("Property not set 'firehoseStreamName'"))

  val messageWorkerQueue: String =
    Option(config.getString("messageWorkerQueue"))
      .getOrElse("Required property 'messageWorkerQueue' not set")
}
