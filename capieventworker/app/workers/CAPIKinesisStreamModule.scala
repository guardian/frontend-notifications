package workers


import javax.inject.{Inject, Singleton}

import play.api.inject.ApplicationLifecycle
import services.{Firehose, Logging}

import scala.concurrent.Future

@Singleton
class CAPIKinesisStreamModule @Inject()(
  applicationLifecycle: ApplicationLifecycle,
  firehose: Firehose) extends Logging {

  import scala.concurrent.ExecutionContext.Implicits.global

  log.info("Starting CAPI Kinesis Stream")
  global.execute(firehose.worker)

  applicationLifecycle.addStopHook { () =>
    Future.successful(firehose.worker.shutdown())
  }
}
