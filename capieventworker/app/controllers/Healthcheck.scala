package controllers

import javax.inject.Singleton

import org.joda.time.DateTime
import play.api.mvc.{Controller, Action}
import services.ServerStatistics

@Singleton
class Healthcheck extends Controller {

  val ConsecutiveErrorThreshold: Int = 10

  def healthcheck() = Action {

    if (ServerStatistics.lastCapiEventReceived.get.exists(lastReceived => DateTime.now.minusHours(1).isBefore(lastReceived))) {
      InternalServerError(s"Have not successfully received a CAPI event since ${ServerStatistics.lastCapiEventReceived}")
    } else if (ServerStatistics.thriftDeserialisationFailures.get >= ConsecutiveErrorThreshold) {
      InternalServerError("Too many consecutive thrift parsing errors")
    } else {
      Ok("Everything is fine")
    }
  }
}
