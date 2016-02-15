package config

import play.Play

case class GCMCredentials(apiKey: String)

object Config {
  private val gcmKey: String =
    Option(Play.application().configuration().getString("gcmKey"))
        .getOrElse(throw new RuntimeException("No gcmKey set"))

  val gcm: GCMCredentials =
    GCMCredentials(gcmKey)

  val gcmSendQueueUrl: Option[String] = Option(Play.application().configuration().getString("gcmSendQueueUrl"))
}
