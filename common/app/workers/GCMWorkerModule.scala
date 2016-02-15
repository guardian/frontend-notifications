package workers

import javax.inject.{Inject, Singleton}

import play.api.inject.ApplicationLifecycle
import services.Logging

import scala.concurrent.Future

@Singleton
class GCMWorkerModule @Inject() (
  applicationLifecycle: ApplicationLifecycle,
  gcmWorker: GCMWorker) extends Logging {

  log.info("Running application lifecycle constructor")
  gcmWorker.start()

  applicationLifecycle.addStopHook { () =>
    Future.successful(gcmWorker.stop())
  }
}
