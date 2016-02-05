Medium SDK for Scala
------
his repository contains the open source SDK for integrating [Medium](https://medium.com/)'s OAuth2 REST API with your Scala app.

For full API documentation, see [developer docs](https://github.com/Medium/medium-api-docs).

## Usage

```scala
import com.shekhargulati.medium.MediumClient
// Go to http://medium.com/me/applications to get your application_id and application_secret.
val medium = MediumClient("test_client_id", "test_client_secret")

// Build the URL where you can send the user to obtain an authorization code.
val authorizationUrl = medium.getAuthorizationUrl("test_state", "https://example.com/me/callback", Array("basicProfile", "listPublications", "publishPost"))

// Exchange the authorization code for an access token.
val accessToken = medium.exchangeAuthorizationCode("authorization_code", "http://www.example.com/")

// Get profile details of the user identified by the access token.
val user = medium.getUser

// Create a draft post

val post = medium.createPost("authorId", PostRequest(title="Introducing Medium.com Scala SDK", contentFormat="markdown", content="# Introducing Medium.com Scala SDK",publishStatus="draft"))

println(s"Created new post with id ${post.id} and title ${post.title}")
```

## Building the project

You can build the project using `sbt`. To run all the test cases, just run `sbt test` task.

## License

Licensed under Apache License Version 2.0. Details in the attached LICENSE file.
