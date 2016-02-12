package services

import java.net.InetAddress
import java.util.UUID
import javax.inject.Singleton

import com.amazonaws.auth.{AWSCredentialsProvider, DefaultAWSCredentialsProviderChain, STSAssumeRoleSessionCredentialsProvider}
import com.amazonaws.services.kinesis.clientlibrary.interfaces.{IRecordProcessor, IRecordProcessorCheckpointer, IRecordProcessorFactory}
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.{InitialPositionInStream, KinesisClientLibConfiguration, Worker}
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownReason
import com.amazonaws.services.kinesis.metrics.impl.NullMetricsFactory
import com.amazonaws.services.kinesis.model.Record
import com.gu.contentapi.client.model.v1.Content
import config.Config
import org.apache.thrift.TDeserializer
import org.joda.time.DateTime

import scala.util.{Failure, Success}

@Singleton
class Firehose {

  val firehoseRole: String = Config.firehoseRole
  val firehoseStreamName: String = Config.firehoseStreamName
  val region: String = "eu-west-1"

  val localCredentials: AWSCredentialsProvider = new DefaultAWSCredentialsProviderChain()

  val firehoseCredentials: AWSCredentialsProvider =
    new STSAssumeRoleSessionCredentialsProvider(
    firehoseRole,
    "firehose")

  val frontendNotifications: String = "capi-firehose-notifications"

  val kinesisClientLibConfiguration = new KinesisClientLibConfiguration(
    frontendNotifications,
    firehoseStreamName,
    firehoseCredentials,
    localCredentials,
    localCredentials,
    InetAddress.getLocalHost.getCanonicalHostName + ":" + UUID.randomUUID())
    .withRegionName(region)
    .withInitialPositionInStream(InitialPositionInStream.LATEST)

  val worker = new Worker(
    RecordProcessorFactory,
    kinesisClientLibConfiguration,
    new NullMetricsFactory()
  )
}

object RecordProcessorFactory extends IRecordProcessorFactory {
  override def createProcessor: IRecordProcessor = new RecordProcessor
}

class RecordProcessor extends IRecordProcessor with Logging {

  private var kinesisShardId: String = _
  private var nextCheckpointTimeInMillis: Long = _
  private val CHECKPOINT_INTERVAL_MILLIS: Long = 1000L

  override def initialize(shardId: String) = {
    println("Initializing record processor for shard: " + shardId)
    this.kinesisShardId = shardId
  }

  override def processRecords(records: java.util.List[Record],
    checkpointer: IRecordProcessorCheckpointer) = {
    println(s"Processing ${records.size} records from $kinesisShardId")

    import scala.collection.JavaConverters._

    log.info(s"Processing ${records.size} records $kinesisShardId")

    records.asScala.foreach { message =>
      ThriftDeserializer.deserializeEvent(message.getData) match {
        case Success(content) =>
          println(content._4.id)
          println(new DateTime(content.dateTime).toString)
          println(content._4.tags.map(_.id).mkString(","))
        case Failure(t) =>
          log.error(s"Could not deserialize message: $t")
          println(s"Could not deserialize message: $t")
      }
    }

    if (System.currentTimeMillis() > nextCheckpointTimeInMillis) {
      checkpointer.checkpoint()
      nextCheckpointTimeInMillis =
        System.currentTimeMillis + CHECKPOINT_INTERVAL_MILLIS
    }
  }

  override def shutdown(checkpointer: IRecordProcessorCheckpointer,
    reason: ShutdownReason) = {
    println(s"Shutting down record processor for shard: $kinesisShardId")
    if (reason == ShutdownReason.TERMINATE) {
      checkpointer.checkpoint()
    }
  }
}
