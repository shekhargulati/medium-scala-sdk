package com.shekhargulati.medium

import com.shekhargulati.medium.MediumApiProtocol._
import com.shekhargulati.medium.domainObjects._
import okhttp3.FormBody.Builder
import okhttp3._
import spray.json._

class MediumClient(clientId: String, clientSecret: String, var accessToken: Option[String] = None) {

  val client = new OkHttpClient()

  val baseApiUrl = new HttpUrl.Builder()
    .scheme("https")
    .host("api.medium.com")
    .build()

  /**
    * Get a URL for users to authorize the application
    *
    * @param state: A string that will be passed back to the redirectUrl
    * @param redirectUrl: The URL to redirect after authorization
    * @param requestScope: The scopes to grant the application
    * @return authorization URL
    */
  def getAuthorizationUrl(state: String, redirectUrl: String, requestScope: Array[String]): String = {
    val httpUrl = baseApiUrl.resolve("/m/oauth/authorize").newBuilder()
      .addQueryParameter("client_id", clientId)
      .addQueryParameter("scope", requestScope.mkString(","))
      .addQueryParameter("state", state)
      .addQueryParameter("response_type", "code")
      .addQueryParameter("redirect_uri", redirectUrl)
      .build()

    httpUrl.toString
  }

  /**
    * Exchange authorization code for a long-lived access token. This allows you to make authenticated requests on behalf of the user.
    *
    * @param code authorization code
    * @return Access token
    */
  def exchangeAuthorizationCode(code: String, redirectUrl: String): AccessToken = {
    val httpUrl = baseApiUrl.resolve("/v1/tokens")

    val body = new Builder()
      .add("code", code)
      .add("client_id", clientId)
      .add("client_secret", clientSecret)
      .add("grant_type", "authorization_code")
      .add("redirect_uri", redirectUrl)
      .build()

    val request = new Request.Builder()
      .header("Content-Type", "application/x-www-form-urlencoded")
      .url(httpUrl)
      .post(body)
      .build()
    val accessTokenObject = makeRequest(request, data => data.convertTo[AccessToken])
    accessToken = Some(accessTokenObject.accessToken)
    accessTokenObject
  }

  /**
    * Get details of the authenticated user
    *
    * @return Returns details of the user who has granted permission to the application.
    */
  def getUser: User = accessToken match {
    case Some(at) => makeRequest(baseApiUrl.resolve("/v1/me"), at, data => data.convertTo[User])
    case _ => mediumError("Please set access token")
  }


  /**
    * Returns a full list of publications that the user is related to in some way: This includes all publications the user is subscribed to, writes to, or edits. This endpoint offers a set of data similar to what you’ll see at https://medium.com/me/publications when logged in.
    * @param userId id of a user
    * @return a sequence of Publication
    */
  def getPublicationsForUser(userId: String): Seq[Publication] = accessToken match {
    case Some(at) => makeRequest(baseApiUrl.resolve(s"/v1/users/$userId/publications"), at, data => data.convertTo[Seq[Publication]])
    case _ => mediumError("Please set access token")
  }

  /**
    * Lists all contributors for a given publication
    *
    * @param publicationId id of the publication.
    * @return a Sequence of contributors
    */
  def getContributorsForPublication(publicationId: String): Seq[Contributor] = accessToken match {
    case Some(at) => makeRequest(baseApiUrl.resolve(s"/v1/publications/$publicationId/contributors"), at, data => data.convertTo[Seq[Contributor]])
    case _ => mediumError("Please set access token")
  }

  /**
    * Creates a post on the authenticated user’s profile
    * @param postRequest post request with data
    * @return created Post
    */
  def createPost(authorId: String, postRequest: PostRequest): Post = accessToken match {
    case Some(at) =>
      val httpUrl = baseApiUrl.resolve(s"/v1/users/$authorId/posts")
      val request = new Request.Builder()
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
        .header("Accept-Charset", "utf-8")
        .header("Authorization", s"Bearer $at")
        .url(httpUrl)
        .post(RequestBody.create(MediaType.parse("application/json"), postRequest.toJson.prettyPrint))
        .build()
      makeRequest(request, data => data.convertTo[Post])
    case _ => mediumError("Please set access token")
  }

  /**
    * Creates a post on Medium and places it under specified publication.
    * Please refer to the API documentation for rules around publishing in
    * a publication: https://github.com/Medium/medium-api-docs
    *
    * @param publicationId
    * @param postRequest
    * @return
    */
  def createPostInPublication(publicationId: String, postRequest: PostRequest): Post = accessToken match {
    case Some(at) =>
      val httpUrl = baseApiUrl.resolve(s"/v1/publications/$publicationId/posts")
      val request = new Request.Builder()
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
        .header("Accept-Charset", "utf-8")
        .header("Authorization", s"Bearer $at")
        .url(httpUrl)
        .post(RequestBody.create(MediaType.parse("application/json"), postRequest.toJson.prettyPrint))
        .build()
      makeRequest(request, data => data.convertTo[Post])
    case _ => mediumError("Please set access token")
  }

  private def makeRequest[T](httpUrl: HttpUrl, at: String, f: (JsValue) => T)(implicit p: JsonReader[T]): T = {
    val request = new Request.Builder()
      .header("Content-Type", "application/json")
      .header("Accept", "application/json")
      .header("Accept-Charset", "utf-8")
      .header("Authorization", s"Bearer $at")
      .url(httpUrl)
      .get()
      .build()
    makeRequest(request, f)
  }

  private def makeRequest[T](request: Request, f: (JsValue) => T)(implicit p: JsonReader[T]): T = {
    val response = client.newCall(request).execute()
    val responseJson: String = response.body().string()
    response match {
      case r if r.isSuccessful =>
        val jsValue: JsValue = responseJson.parseJson
        jsValue.asJsObject.getFields("data").headOption match {
          case Some(data) => f(data)
          case _ => jsValue.convertTo[T]
        }
      case _ => mediumError(s"Received HTTP error response code ${response.code()}" + responseJson.parseJson.convertTo[ErrorResponse].toString)
    }
  }

}

object MediumClient {
  def apply(clientId: String, clientSecret: String): MediumClient = new MediumClient(clientId, clientSecret)

  def apply(clientId: String, clientSecret: String, accessToken: String): MediumClient = new MediumClient(clientId, clientSecret, Some(accessToken))
}


case class MediumException(message: String, cause: Throwable = null) extends RuntimeException(message, cause)

