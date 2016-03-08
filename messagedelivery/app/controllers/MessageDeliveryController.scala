package controllers

import javax.inject.Singleton

import play.api.mvc.{Action, Controller}

@Singleton
class MessageDeliveryController extends Controller {

  def index = Action {
    Ok("Index OK from Message Delivery")
  }

}
