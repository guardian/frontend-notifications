package workers

import javax.inject.{Inject, Singleton}

import play.api.inject.ApplicationLifecycle
import services.Logging

import scala.concurrent.Future


@Singleton
class MessageWorkerModule @Inject() (
  applicationLifecycle: ApplicationLifecycle,
  messageWorker: MessageWorker) extends Logging {

  log.info("Running application lifecycle constructor")
  messageWorker.start()

  applicationLifecycle.addStopHook { () =>
    Future.successful(messageWorker.stop())
  }
}
