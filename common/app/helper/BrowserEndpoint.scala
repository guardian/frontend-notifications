package helper

sealed trait BrowserEndpoint
case class ChromeEndpoint(get: String) extends BrowserEndpoint
case class FirefoxEndpoint(get: String) extends BrowserEndpoint

//Temporary until we move completely to endpoints
case class GcmId(get: String) extends BrowserEndpoint

object BrowserEndpoint {
  private val ChromeEndpointPattern: String = "https://android.googleapis.com/gcm/send"
  private val FirefoxEndpointPattern: String = "https://updates.push.services.mozilla.com/push"

  def fromEndpointUrl(endpointUrl: String): Option[BrowserEndpoint] =
    endpointUrl match {
      case endpoint if endpoint.startsWith(ChromeEndpointPattern) => Option(ChromeEndpoint(endpoint))
      case endpoint if endpoint.startsWith(FirefoxEndpointPattern) => Option(FirefoxEndpoint(endpoint))
      case endpointUrl: String if !endpointUrl.startsWith("http") => Option(GcmId(endpointUrl))
      case _ => None
    }
}

object ChromeEndpoint {
  def toGcmId(chromeEndpoint: ChromeEndpoint): Option[GcmId] =
    chromeEndpoint.get.split('/').lastOption.map(GcmId)
}