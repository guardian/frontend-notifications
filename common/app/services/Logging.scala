package services

import play.api.Logger

trait Logging {
  implicit val log = Logger(getClass)
}
