package services

import java.util.UUID
import java.util.concurrent.atomic.{AtomicLong, AtomicReference}
import javax.inject.{Inject, Singleton}

import com.amazonaws.auth.{AWSCredentialsProvider, DefaultAWSCredentialsProviderChain, STSAssumeRoleSessionCredentialsProvider}
import com.amazonaws.services.kinesis.clientlibrary.interfaces.{IRecordProcessor, IRecordProcessorCheckpointer, IRecordProcessorFactory}
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.{InitialPositionInStream, KinesisClientLibConfiguration, Worker}
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownReason
import com.amazonaws.services.kinesis.metrics.impl.NullMetricsFactory
import com.amazonaws.services.kinesis.model.Record
import com.gu.crier.model.event.v1.EventPayload.{UnknownUnionField, Content}
import config.Config
import model.{KeyEvent, LiveBlogUpdateEvent}
import workers.MessageWorker

import scala.collection.JavaConverters._
import scala.util.{Failure, Success}

@Singleton
class Firehose @Inject() (config: Config, messageWorker: MessageWorker) {

  val localCredentials: AWSCredentialsProvider = new DefaultAWSCredentialsProviderChain()

  val stsRoleSessionName: String = "firehose"
  val firehoseCredentials: AWSCredentialsProvider =
    new STSAssumeRoleSessionCredentialsProvider(
      localCredentials,
      config.firehoseRole,
      stsRoleSessionName)

  val kinesisClientLibConfiguration = new KinesisClientLibConfiguration(
    config.firehoseStreamLeaseTableName,
    config.firehoseStreamName,
    firehoseCredentials,
    localCredentials,
    localCredentials,
    UUID.randomUUID().toString)
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

    ServerStatistics.capiEventsReceived.addAndGet(records.size.toLong)

    records.asScala.foreach { message =>
      ThriftDeserializer.deserializeEvent(message.getData) match {
        case Success(event) =>
          ServerStatistics.lastCapiEventReceived.refresh()
          ServerStatistics.thriftDeserialisationFailures.set(0L)

          event.payload match {
            case Some(Content(content)) =>
              val tags: List[String] = content.tags.map(_.id).toList
              if (tags.exists(_ == "tone/minutebyminute")) {
                ServerStatistics.capiEventsProcessed.incrementAndGet()
                log.info(s"Putting ${content.id} onto queue!")
                messageWorker.queue.send(LiveBlogUpdateEvent(content.id, KeyEvent.fromContent(content)))}
            case Some(UnknownUnionField(unknown)) =>
              log.warn(s"Got an unknown payload type from CAPI: $unknown")
            case None =>
              log.warn(s"Got an event with NO payload")}
        case Failure(t) =>
          ServerStatistics.thriftDeserialisationFailures.incrementAndGet()
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
