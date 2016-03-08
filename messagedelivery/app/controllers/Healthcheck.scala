package controllers

import javax.inject.Singleton

import play.api.mvc.{Controller, Action}

@Singleton
class Healthcheck extends Controller {

  def healthcheck() = Action {
    Ok("Ok")
  }
}
