package controllers

import javax.inject.{Inject, Singleton}

import play.api.mvc.{Action, Controller}
import services.ServerStatistics
import workers.CAPIKinesisStreamModule

@Singleton
class CapiEventWorkerApplication @Inject() (capiEventWorker: CAPIKinesisStreamModule) extends Controller {

  def index = Action {
    Ok("Index OK from CAPI Event Worker")
  }

  def info = Action {
    Ok(s"CAPI events Received: ${ServerStatistics.capiEventsReceived.get()}\n" +
      s"CAPI Events Processed: ${ServerStatistics.capiEventsProcessed.get()}\n" +
      s"Last Event Received: ${ServerStatistics.lastCapiEventReceived.get}" +
      s"Thrift Deserialisation Errors: ${ServerStatistics.thriftDeserialisationFailures}")
  }
}
