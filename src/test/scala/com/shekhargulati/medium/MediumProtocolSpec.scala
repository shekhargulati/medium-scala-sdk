package com.shekhargulati.medium

import com.shekhargulati.medium.MediumApiProtocol._
import com.shekhargulati.medium.domainObjects.AccessToken
import org.scalatest.{FunSpec, Matchers}
import spray.json._

class MediumProtocolSpec extends FunSpec with Matchers {

  describe("Medium Protocol") {
    it("should convert access token response to AccessToken") {
      val json =
        """
          |{
          |  "token_type": "Bearer",
          |  "access_token": "test-access-token",
          |  "refresh_token": "test-refresh-token",
          |  "scope": [
          |    "basicProfile",
          |    "listPublications",
          |    "publishPost"
          |  ],
          |  "expires_at": 1459842647035
          |}
        """.stripMargin

      val accessToken = json.parseJson.convertTo[AccessToken]
      accessToken should have(
        'tokenType ("Bearer"),
        'accessToken ("test-access-token"),
        'refreshToken ("test-refresh-token"),
        'expiresAt (1459842647035L)
      )
      accessToken.scope should be(Array("basicProfile", "listPublications", "publishPost"))

    }

  }

}