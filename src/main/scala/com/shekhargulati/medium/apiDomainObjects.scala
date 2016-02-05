package com.shekhargulati.medium

object apiDomainObjects {

  case class AccessToken(tokenType: String, accessToken: String, refreshToken: String, scope: Array[String], expiresAt: Long)

  case class User(id: String, username: String, name: String, url: String, imageUrl: String)

  case class Publication(id: String, name: String, description: String, url: String, imageUrl: String)

  case class PostRequest(title: String, contentFormat: String, content: String, tags: Array[String] = Array(), canonicalUrl: Option[String] = None, publishStatus: String = "public", license: String = "all-rights-reserved")

  case class Post(id: String, publicationId: Option[String] = None, title: String, authorId: String, tags: Array[String], url: String, canonicalUrl: String, publishStatus: String, publishedAt: Long, license: String, licenseUrl: String)

  case class Contributor(publicationId: String, userId: String, role: String)

  case class ErrorResponse(errors: Array[ErrorMessage]) {
    override def toString: String = errors.map(_.toString).mkString("\n")
  }

  case class ErrorMessage(message: String, code: Int)

}
