package services

import java.net.InetAddress
import java.util.UUID
import java.util.concurrent.atomic.{AtomicReference, AtomicLong}
import javax.inject.{Inject, Singleton}

import com.amazonaws.auth.{AWSCredentialsProvider, DefaultAWSCredentialsProviderChain, STSAssumeRoleSessionCredentialsProvider}
import com.amazonaws.services.kinesis.clientlibrary.interfaces.{IRecordProcessor, IRecordProcessorCheckpointer, IRecordProcessorFactory}
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.{InitialPositionInStream, KinesisClientLibConfiguration, Worker}
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownReason
import com.amazonaws.services.kinesis.metrics.impl.NullMetricsFactory
import com.amazonaws.services.kinesis.model.Record
import config.Config
import org.joda.time.DateTime
import workers.{MessageWorker, PublishedMessage}

import scala.util.{Failure, Success}
import scala.collection.JavaConverters._

@Singleton
class Firehose @Inject() (config: Config, messageWorker: MessageWorker) {

  val localCredentials: AWSCredentialsProvider = new DefaultAWSCredentialsProviderChain()

  val stsRoleSessionName: String = "firehose"
  val firehoseCredentials: AWSCredentialsProvider =
    new STSAssumeRoleSessionCredentialsProvider(
      config.firehoseRole,
      stsRoleSessionName)

  val frontendNotifications: String = "capi-firehose-notifications"

  val kinesisClientLibConfiguration = new KinesisClientLibConfiguration(
    frontendNotifications,
    config.firehoseStreamName,
    firehoseCredentials,
    localCredentials,
    localCredentials,
    InetAddress.getLocalHost.getCanonicalHostName + ":" + UUID.randomUUID())
    .withRegionName(config.firehoseRegionName)
    .withInitialPositionInStream(InitialPositionInStream.LATEST)

  val worker = new Worker(
    new RecordProcessorFactory(messageWorker),
    kinesisClientLibConfiguration,
    new NullMetricsFactory()
  )
}

class RecordProcessorFactory(messageWorker: MessageWorker) extends IRecordProcessorFactory {
  override def createProcessor: IRecordProcessor = new RecordProcessor(messageWorker)
}

class RecordProcessor(messageWorker: MessageWorker) extends IRecordProcessor with Logging {

  private val kinesisShardId: AtomicReference[String] = new AtomicReference("")
  private val nextCheckpointTimeInMillis: AtomicLong = new AtomicLong(0L)
  private val CHECKPOINT_INTERVAL_MILLIS: Long = 1000L

  override def initialize(shardId: String) = {
    log.info("Initializing record processor for shard: " + shardId)
    kinesisShardId.set(shardId)
  }

  override def processRecords(
    records: java.util.List[Record],
    checkpointer: IRecordProcessorCheckpointer): Unit = {

    log.info(s"Processing ${records.size} records from $kinesisShardId")

    records.asScala.foreach { message =>
      ThriftDeserializer.deserializeEvent(message.getData) match {
        case Success(content) =>
          val tags: List[String] = content._4.tags.map(_.id).toList
          if (tags.exists(_ == "tone/minutebyminute")) {
            log.info(s"Putting ${content._4.id} onto queue!")
            messageWorker.queue.send(PublishedMessage(content._4.id))}
        case Failure(t) =>
          log.error(s"Could not deserialize message: $t")
      }
    }

    if (System.currentTimeMillis() > nextCheckpointTimeInMillis.get()) {
      checkpointer.checkpoint()
      nextCheckpointTimeInMillis.set(System.currentTimeMillis + CHECKPOINT_INTERVAL_MILLIS)
    }
  }

  override def shutdown(
    checkpointer: IRecordProcessorCheckpointer,
    reason: ShutdownReason) = {
    log.info(s"Shutting down record processor for shard: $kinesisShardId")
    if (reason == ShutdownReason.TERMINATE) {
      checkpointer.checkpoint()
    }
  }
}