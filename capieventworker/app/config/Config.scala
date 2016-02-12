package config

import com.typesafe.config.ConfigFactory
import play.Play

object Config {
  lazy val configFactory = ConfigFactory.load()

  lazy val capiKinesisStream: String =
    Option(configFactory.getString("capiKinesisStream"))
        .getOrElse(throw new RuntimeException("Property not set 'capiKinesisStream'"))

  lazy val firehoseRole: String =
    Option(configFactory.getString("firehoseRole"))
        .getOrElse(throw new RuntimeException("Property not set 'firehoseRole'"))

  lazy val firehoseStreamName: String =
    Option(configFactory.getString("firehoseStreamName"))
        .getOrElse(throw new RuntimeException("Property not set 'firehoseStreamName'"))
}
