package com.shekhargulati.medium

import com.shekhargulati.medium.domainObjects._
import spray.json._

object MediumApiProtocol extends DefaultJsonProtocol {

  implicit val accessTokenFormat = jsonFormat(AccessToken, "token_type", "access_token", "refresh_token", "scope", "expires_at")

  implicit val userFormat = jsonFormat5(User)

  implicit val publicationFormat = jsonFormat5(Publication)

  implicit val contributorFormat = jsonFormat3(Contributor)

  implicit val postRequestFormat = jsonFormat7(PostRequest)

  implicit val postFormat = jsonFormat11(Post)

  implicit val errorMessageFormat = jsonFormat2(ErrorMessage)

  implicit val errorResponseFormat = jsonFormat1(ErrorResponse)

}
