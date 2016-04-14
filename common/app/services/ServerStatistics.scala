package services

import java.util.concurrent.atomic.AtomicLong

object ServerStatistics {
  val capiEventsReceived: AtomicLong = new AtomicLong(0L)
  val capiEventsProcessed: AtomicLong = new AtomicLong(0L)
  val recordsProcessed: AtomicLong = new AtomicLong(0L)
  val gcmMessagesSent: AtomicLong = new AtomicLong(0L)

  val lastCapiEventReceived: DateTimeRecorder = new DateTimeRecorder
  val thriftDeserialisationFailures: AtomicLong = new AtomicLong(0L)
}
