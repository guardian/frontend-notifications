package helper

import org.scalatest.{Matchers, FreeSpec}

class BrowserEndpointTest extends FreeSpec with Matchers {

  "BrowserEndpoint" - {
    "returns ChromeEndpoint for valid chrome endpoint" in {
      val validChromeEndpointUrl: String = "https://android.googleapis.com/gcm/send/vpn7kxGinVW9kL7ZJJZk:AAaMXNm08HpePuAfaVRmgn7xClg9pGbO98AuP7xapURTeFx_33DQt9lKWds0uaQnZv3SOy53_SZ18bMsKwN2"

      BrowserEndpoint.fromEndpointUrl(validChromeEndpointUrl) should be (Some(ChromeEndpoint(validChromeEndpointUrl)))
    }

    "returns None for invalid chrome endpoint" in {
      val validChromeEndpointUrl: String = "https://android.googleapis.com/gcm/vpn7kxGinVW9kL7ZJJZk:AAaMXNm08HpePuAfaVRmgn7xClg9pGbO98AuP7xapURTeFx_33DQt9lKWds0uaQnZv3SOy53_SZ18bMsKwN2"

      BrowserEndpoint.fromEndpointUrl(validChromeEndpointUrl) should be (None)
    }

    "returns FirefoxEndpoint for a valid firefox endpoint" in {
      val validFirefoxEndpoint: String = "https://updates.push.services.mozilla.com/push/v1/pi5CQWx8reLySO0cgAAABXC87GxcojoBe7Si9M1a7sr2Dn05NGtPA4uMVBukyk8AVj0PMrmn19i-Pv2VeCGx5L4B_OZR34"

      BrowserEndpoint.fromEndpointUrl(validFirefoxEndpoint) should be (Some(FirefoxEndpoint(validFirefoxEndpoint)))
    }

    "returns None for a invalid firefox endpoint" in {
      val validFirefoxEndpoint: String = "https://updates.push.services.mozilla.com/pi5CQWx8reLySO0cgAAABXC87GxcojoBe7Si9M1a7sr2Dn05NGtPA4uMVBukyk8AVj0PMrmn19i-Pv2VeCGx5L4B_OZR34"

      BrowserEndpoint.fromEndpointUrl(validFirefoxEndpoint) should be (None)
    }
  }
}
