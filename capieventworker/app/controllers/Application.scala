package controllers

import javax.inject.{Inject, Singleton}

import play.api.mvc.{Action, Controller}
import workers.CAPIKinesisStreamModule

@Singleton
class Application @Inject() (capiEventWorker: CAPIKinesisStreamModule) extends Controller {

  def index = Action {
    Ok("Index OK from CAPI Event Worker")
  }
}
