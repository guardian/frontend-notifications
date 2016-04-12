package helper

import org.scalatest.{Matchers, FreeSpec}

class ChromeEndpointTest extends FreeSpec with Matchers {

  "ChromeEndpoint" - {
    "returns proper ID for a valid chrome endpoint" in {
      val validChromeEndpoint: String = "https://android.googleapis.com/gcm/send/vpn7kxGinVW9kL7ZJJZk:AAaMXNm08HpePuAfaVRmgn7xClg9pGbO98AuP7xapURTeFx_33DQt9lKWds0uaQnZv3SOy53_SZ18bMsKwN2"
      val validChromeId: String = "vpn7kxGinVW9kL7ZJJZk:AAaMXNm08HpePuAfaVRmgn7xClg9pGbO98AuP7xapURTeFx_33DQt9lKWds0uaQnZv3SOy53_SZ18bMsKwN2"

      ChromeEndpoint.toGcmId(ChromeEndpoint(validChromeEndpoint)) should be (Some(GcmId(validChromeId)))
    }
  }
}
