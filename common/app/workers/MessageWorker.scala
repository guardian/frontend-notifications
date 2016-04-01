package workers

import javax.inject.{Inject, Singleton}

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.regions.{Regions, Region}
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClient
import com.amazonaws.services.sqs.AmazonSQSAsyncClient
import config.Config
import helper.DynamoLockingUpdateTable
import model.{KeyEvent, PublishedMessage, LiveBlogUpdateEvent, Update}
import org.joda.time.DateTime
import services._

import scala.concurrent.Future

@Singleton
class MessageWorker @Inject() (
  config: Config,
  gcmWorker: GCMWorker,
  redisMessageDatabaseModule: RedisMessageDatabaseModule,
  clientDatabase: ClientDatabase) extends JsonQueueWorker[Update] with Logging {
  import scala.concurrent.ExecutionContext.Implicits.global

  val dynamoClient: AmazonDynamoDBAsyncClient = new AmazonDynamoDBAsyncClient().withRegion(Region.getRegion(Regions.EU_WEST_1))
  val lockingUpdate: DynamoLockingUpdateTable[LastSent] = new DynamoLockingUpdateTable[LastSent](
    dynamoClient,
    config.LastSentTableName,
    "topicId",
    "dataKey")

  val redisMessageDatabase: RedisMessageDatabase = redisMessageDatabaseModule.redisMessageDatabase

  override val queue: JsonMessageQueue[Update] =
    JsonMessageQueue[Update](
      new AmazonSQSAsyncClient(
        new DefaultAWSCredentialsProviderChain()).withRegion(config.workerQueueRegion),
      config.messageWorkerQueue)

  override def process(message: SQSMessage[Update]): Future[Unit] = {
    ServerStatistics.recordsProcessed.incrementAndGet()
    message.get match {
      case PublishedMessage(topic: String) =>
        log.info(s"Processing job for PublishedMessage($topic)")
        Future.successful(())

      case LiveBlogUpdateEvent(topic, keyEvents) =>
        log.info(s"Processing job for LiveBlogUpdateEvent($topic)(${keyEvents.length} key events)")

        lazy val emptyLastSentKeyEvent: LastSentKeyEvent = LastSentKeyEvent(topic, DateTime.now(), keyEvents.lastOption.map(_.id))

        lockingUpdate.lockingReadAndWriteWithCondition(id=topic, empty=emptyLastSentKeyEvent){
          case ke@LastSentKeyEvent(t, dateTime, Some(lastKeyEventId)) =>
            log.info(s"Last sent to $topic at $dateTime with lastKeyEventId: $lastKeyEventId")
            KeyEvent.getLastestKeyEvents(lastKeyEventId, keyEvents).lastOption.map(lastKeyEvent => LastSentKeyEvent(t, DateTime.now(), Option(lastKeyEvent.id)))
          case ke@LastSentKeyEvent(t, dateTime, None) =>
            log.info(s"Never sent to topic $topic before")
            keyEvents.lastOption.map(keyEvent => LastSentKeyEvent(t, DateTime.now(), Some(keyEvent.id)))
          case t =>
            log.warn(s"Got the wrong type for $topic: $t")
            None}
        .map {
          case lockingUpdate.ReadAndWrite(LastSentKeyEvent(t, _, Some(lastKeyEventId)), newItem) =>
            val newKeyEvents: List[KeyEvent] = KeyEvent.getLastestKeyEvents(lastKeyEventId, keyEvents)
            log.info(s"Sending ${newKeyEvents.length} new events to $topic (Out of ${keyEvents.length} possible events)")
            sendKeyEvents(t, newKeyEvents)
          case lockingUpdate.NewItem(LastSentKeyEvent(t, _, _)) =>
            log.info(s"Never seen $t before; sending all ${keyEvents.length} key events")
            sendKeyEvents(t, keyEvents)
          case t =>
            log.warn(s"Did not sent any events for $topic: DB Result: $t")}}
  }

  private def sendKeyEvents(topic: String, keyEvents: List[KeyEvent]): Future[Unit] =
    clientDatabase.getIdsByTopic(topic).map { listOfBrowserIds =>
      log.info(s"There are ${listOfBrowserIds.size} browers to notify for $topic")
      listOfBrowserIds.foreach { browserId =>
        keyEvents.map { keyEvent =>
          val topicMessage: String = keyEvent.title.getOrElse(s"Message for $topic")
          log.info(s"Message for $topic with Id: ${keyEvent.id}")
          val gcmMessage: GCMMessage = GCMMessage(browserId.get, topic, topicMessage, keyEvent.body, keyEvent.id)
          redisMessageDatabase.leaveMessageWithDefaultExpiry(gcmMessage).map { _ =>
            ServerStatistics.gcmMessagesSent.incrementAndGet()
            gcmWorker.queue.send(List(gcmMessage))}}}}

}
