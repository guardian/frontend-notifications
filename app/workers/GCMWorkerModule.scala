package workers

import javax.inject.{Inject, Singleton}

import play.api.inject.ApplicationLifecycle
import services.Logging

import scala.concurrent.Future

@Singleton
class GCMWorkerModule @Inject() (applicationLifecycle: ApplicationLifecycle) extends Logging {

  log.info("Running application lifecycle constructor")
  println("Running application lifecycle constructor")
  GCMWorker.start()

  applicationLifecycle.addStopHook { () =>
    Future.successful(GCMWorker.stop())
  }
}
