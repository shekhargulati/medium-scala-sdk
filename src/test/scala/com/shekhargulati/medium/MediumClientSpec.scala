package com.shekhargulati.medium

import com.shekhargulati.medium.domainObjects.PostRequest
import okhttp3.mockwebserver.{MockResponse, MockWebServer}
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}

class MediumClientSpec extends FunSpec with Matchers with BeforeAndAfterEach {


  var server: MockWebServer = _

  override protected def beforeEach(): Unit = {
    server = new MockWebServer()
  }

  override protected def afterEach(): Unit = {
    server.shutdown()
  }


  describe("Medium Client") {

    describe("when getAuthorizationUrl is called") {

      it("should generate valid authorization URL") {
        val medium = MediumClient("test_client_id", "test_client_secret")
        val authorizationUrl = medium.getAuthorizationUrl("test_state", "https://example.com/me/callback", Array("basicProfile", "listPublications", "publishPost"))
        authorizationUrl should be("https://api.medium.com/m/oauth/authorize?client_id=test_client_id&scope=basicProfile,listPublications,publishPost&state=test_state&response_type=code&redirect_uri=https://example.com/me/callback")
      }
    }

    describe("when exchange access token for code is successful") {
      def fixture = new {
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
        server.enqueue(
          new MockResponse()
            .setBody(json)
            .setHeader("Content-Type", "application/json")
            .setHeader("charset", "utf-8")
        )
        server.start()

        val medium = new MediumClient("test_client_id", "test_client_secret") {
          override val baseApiUrl = server.url("/v1/tokens")
        }
      }

      it("then access token should be returned") {
        val f = fixture
        val accessToken = f.medium.exchangeAuthorizationCode("authorization_code", "http://www.example.com/")
        accessToken should have(
          'tokenType ("Bearer"),
          'accessToken ("test-access-token"),
          'refreshToken ("test-refresh-token"),
          'expiresAt (1459842647035L)
        )
        accessToken.scope should be(Array("basicProfile", "listPublications", "publishPost"))
      }

      it("then access token should be set in the medium client") {
        val f = fixture
        f.medium.exchangeAuthorizationCode("authorization_code", "http://www.example.com/")
        f.medium.accessToken should be(Some("test-access-token"))
      }

    }

    describe("when exchange access token for code gives error") {

      def fixture = new {
        val json =
          """
            |{
            |  "errors": [
            |    {
            |      "message": "Authorization code not found.",
            |      "code": 6016
            |    }
            |  ]
            |}
          """.stripMargin

        server.enqueue(
          new MockResponse()
            .setBody(json)
            .setResponseCode(401)
            .setHeader("Content-Type", "application/json")
            .setHeader("charset", "utf-8")
        )
        server.start()

        val medium = new MediumClient("test_client_id", "test_client_secret") {
          override val baseApiUrl = server.url("/v1/tokens")
        }
      }

      it("then MediumError should be thrown") {
        val f = fixture
        the[MediumException] thrownBy {
          f.medium.exchangeAuthorizationCode("test-code", "https://example.com/")
        }
      }
    }

    describe("when getting details of authenticated user") {

      def fixture = new {
        var json =
          """
            |{
            |  "data": {
            |    "id": "123",
            |    "username": "shekhargulati",
            |    "name": "Shekhar Gulati",
            |    "url": "https://medium.com/@shekhargulati",
            |    "imageUrl": "https://cdn-images-1.medium.com/fit/c/200/200/1*pC-eYQUV-iP2Y10_LgGvwA.jpeg"
            |  }
            |}
          """.stripMargin

        server.enqueue(new MockResponse()
          .setBody(json)
          .setHeader("Content-Type", "application/json")
          .setHeader("charset", "utf-8"))
        server.start()
      }

      it("then User details should be returned when access token is passed with request") {
        val f = fixture
        val medium = new MediumClient("test_client_id", "test_client_secret", Some("access_token")) {
          override val baseApiUrl = server.url("/v1/me")
        }
        val user = medium.getUser
        user should have(
          'id ("123"),
          'username ("shekhargulati"),
          'name ("Shekhar Gulati"),
          'url ("https://medium.com/@shekhargulati"),
          'imageUrl ("https://cdn-images-1.medium.com/fit/c/200/200/1*pC-eYQUV-iP2Y10_LgGvwA.jpeg")
        )
      }

      it("error should be thrown when access token is not provided") {
        val f = fixture
        val medium = new MediumClient("test_client_id", "test_client_secret") {
          override val baseApiUrl = server.url("/v1/me")
        }
        the[MediumException] thrownBy medium.getUser
      }

      def errorFixture = new {
        var json =
          """
            |{
            |  "errors": [
            |    {
            |      "message": "Token was invalid.",
            |      "code": 6003
            |    }
            |  ]
            |}
          """.stripMargin

        server.enqueue(new MockResponse()
          .setBody(json)
          .setHeader("Content-Type", "application/json")
          .setHeader("charset", "utf-8")
          .setResponseCode(401)
        )
        server.start()
      }

      it("error should be thrown when access token is invalid") {
        val f = errorFixture
        val medium = new MediumClient("test_client_id", "test_client_secret", Some("addd")) {
          override val baseApiUrl = server.url("/v1/me")
        }
        the[MediumException] thrownBy medium.getUser
      }
    }

    describe("when getting publications for a user") {

      def fixture = new {
        var json =
          """
            |{
            |  "data": [
            |    {
            |      "id": "668e14b18fb1",
            |      "name": "Signal v. Noise",
            |      "description": "Strong opinions and shared thoughts",
            |      "url": "https://medium.com/signal-v-noise",
            |      "imageUrl": "https://cdn-images-1.medium.com/fit/c/200/200/1*UUpa5mFtnLRLlT3nzi4FjQ.png"
            |    },
            |    {
            |      "id": "8d596e6c8706",
            |      "name": "The Realm",
            |      "description": "Inspiration from daily lives, creating pieces of philosophy, writing stories of travel",
            |      "url": "https://medium.com/the-realm",
            |      "imageUrl": "https://cdn-images-1.medium.com/fit/c/200/200/1*nSftJwCIFSBsBiTeC1WrEg.jpeg"
            |    },
            |    {
            |      "id": "bcc38c8f6edf",
            |      "name": "Matter",
            |      "description": "The story you?ve been missing.",
            |      "url": "https://medium.com/matter",
            |      "imageUrl": "https://cdn-images-1.medium.com/fit/c/200/200/1*Pr4CiPNL0HVDAUsq7agsIg.png"
            |    },
            |    {
            |      "id": "e358ae9bba4a",
            |      "name": "Editor?s Picks",
            |      "description": "Stories we think more people should notice. Edited by Medium staff. (At this time, we do not review unsolicited submissions.)",
            |      "url": "https://medium.com/editors-picks",
            |      "imageUrl": "https://cdn-images-1.medium.com/fit/c/200/200/1*3rvDqTClQaUVmFANOUgMmw.jpeg"
            |    }
            |  ]
            |}
          """.stripMargin

        server.enqueue(new MockResponse()
          .setBody(json)
          .setHeader("Content-Type", "application/json")
          .setHeader("charset", "utf-8"))
        server.start()
      }

      it("a sequence of publications should be returned") {
        val f = fixture
        val medium = new MediumClient("test_client_id", "test_client_secret", Some("access_token")) {
          override val baseApiUrl = server.url("/v1/users/test_user/publications")
        }

        val publications = medium.getPublicationsForUser("test_user")
        publications should have length 4
        publications.map(_.id) should be(Seq("668e14b18fb1", "8d596e6c8706", "bcc38c8f6edf", "e358ae9bba4a"))
      }
    }

    describe("when getting contributors for a publication") {
      def fixture = new {
        var json =
          """
            |{
            |  "data": [
            |    {
            |      "publicationId": "b45573563f5a",
            |      "userId": "13a06af8f81849c64dafbce822cbafbfab7ed7cecf82135bca946807ea351290d",
            |      "role": "editor"
            |    },
            |    {
            |      "publicationId": "b45573563f5a",
            |      "userId": "1c9c63b15b874d3e354340b7d7458d55e1dda0f6470074df1cc99608a372866ac",
            |      "role": "editor"
            |    },
            |    {
            |      "publicationId": "b45573563f5a",
            |      "userId": "1cc07499453463518b77d31650c0b53609dc973ad8ebd33690c7be9236e9384ad",
            |      "role": "editor"
            |    },
            |    {
            |      "publicationId": "b45573563f5a",
            |      "userId": "196f70942410555f4b3030debc4f199a0d5a0309a7b9df96c57b8ec6e4b5f11d7",
            |      "role": "writer"
            |    },
            |    {
            |      "publicationId": "b45573563f5a",
            |      "userId": "14d4a581f21ff537d245461b8ff2ae9b271b57d9554e25d863e3df6ef03ddd480",
            |      "role": "writer"
            |    }
            |  ]
            |}
          """.stripMargin

        server.enqueue(new MockResponse()
          .setBody(json)
          .setHeader("Content-Type", "application/json")
          .setHeader("charset", "utf-8"))
        server.start()
      }

      it("a sequence of contributor should be returned") {
        val f = fixture
        val medium = new MediumClient("test_client_id", "test_client_secret", Some("access_token")) {
          override val baseApiUrl = server.url("/v1/publications/my_publication_id/contributors")
        }

        val contributors = medium.getContributorsForPublication("my_publication_id")
        contributors should have length 5
      }

    }

    describe("when publishing a post") {

      def fixture = new {
        val responsJson =
          """
            |{
            | "data": {
            |   "id": "e6f36a",
            |   "title": "Liverpool FC",
            |   "authorId": "5303d74c64f66366f00cb9b2a94f3251bf5",
            |   "tags": ["football", "sport", "Liverpool"],
            |   "url": "https://medium.com/@majelbstoat/liverpool-fc-e6f36a",
            |   "canonicalUrl": "http://jamietalbot.com/posts/liverpool-fc",
            |   "publishStatus": "public",
            |   "publishedAt": 1442286338435,
            |   "license": "all-rights-reserved",
            |   "licenseUrl": "https://medium.com/policy/9db0094a1e0f"
            | }
            |}
          """.stripMargin

        server.enqueue(new MockResponse()
          .setBody(responsJson)
          .setHeader("Content-Type", "application/json")
          .setHeader("charset", "utf-8"))
        server.start()
      }

      it("should publish a new post") {
        val f = fixture
        val medium = new MediumClient("test_client_id", "test_client_secret", Some("access_token")) {
          override val baseApiUrl = server.url("/v1/users/123/posts")
        }

        val content =
          """
            |# Hello World
            |Hello how are you?
            |## What's up today?
            |Writing REST client for Medium API
          """.stripMargin
        val post = medium.createPost("123", PostRequest("Liverpool FC", "html", content))

        println(post)
        post.id should be("e6f36a")

      }
    }

    describe("when publishing a post under publication") {

      def fixture = new {
        val responsJson =
          """
            |{
            | "data": {
            |   "id": "e6f36a",
            |   "publicationId" :"e6f36a23",
            |   "title": "Liverpool FC",
            |   "authorId": "5303d74c64f66366f00cb9b2a94f3251bf5",
            |   "tags": ["football", "sport", "Liverpool"],
            |   "url": "https://medium.com/@majelbstoat/liverpool-fc-e6f36a",
            |   "canonicalUrl": "http://jamietalbot.com/posts/liverpool-fc",
            |   "publishStatus": "public",
            |   "publishedAt": 1442286338435,
            |   "license": "all-rights-reserved",
            |   "licenseUrl": "https://medium.com/policy/9db0094a1e0f"
            | }
            |}
          """.stripMargin

        server.enqueue(new MockResponse()
          .setBody(responsJson)
          .setHeader("Content-Type", "application/json")
          .setHeader("charset", "utf-8"))
        server.start()
      }

      it("should publish a new post") {
        val f = fixture
        val medium = new MediumClient("test_client_id", "test_client_secret", Some("access_token")) {
          override val baseApiUrl = server.url("/v1/publications/e6f36a23/posts")
        }

        val content =
          """
            |# Hello World
            |Hello how are you?
            |## What's up today?
            |Writing REST client for Medium API
          """.stripMargin
        val post = medium.createPostInPublication("123", PostRequest("Liverpool FC", "html", content))
        println(post)
        post.id should be("e6f36a")
        post.publicationId should be(Some("e6f36a23"))
      }
    }


  }

}