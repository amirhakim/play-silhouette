/**
 * Copyright 2015 Mohiva Organisation (license at mohiva dot com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mohiva.play.silhouette.impl.services

import com.mohiva.play.silhouette.api.util.HTTPLayer
import com.mohiva.play.silhouette.impl.services.GravatarService._
import org.specs2.mock.Mockito
import org.specs2.specification.Scope
import play.api.libs.ws.{ WSRequest, WSResponse }
import play.api.test.PlaySpecification
import play.api.libs.concurrent.Execution._

import scala.concurrent.Future

/**
 * Test case for the [[com.mohiva.play.silhouette.impl.services.GravatarService]] class.
 */
class GravatarServiceSpec extends PlaySpecification with Mockito {

  "The `retrieveURL` method" should {
    "return None if email is empty" in new Context {
      await(service.retrieveURL("")) should beNone
    }

    "return None if HTTP status code isn't 200" in new Context {
      val requestHolder = mock[WSRequest]
      val response = mock[WSResponse]

      response.status returns 404
      requestHolder.get() returns Future.successful(response)
      httpLayer.executionContext returns defaultContext
      httpLayer.url(any) returns requestHolder

      await(service.retrieveURL(email)) should beNone
    }

    "return None if HTTP status code isn't 200" in new Context {
      val requestHolder = mock[WSRequest]
      val response = mock[WSResponse]

      response.status returns 404
      requestHolder.get() returns Future.successful(response)
      httpLayer.url(any) returns requestHolder

      await(service.retrieveURL(email)) should beNone
    }

    "return None if exception will be thrown during API request" in new Context {
      val requestHolder = mock[WSRequest]
      val response = mock[WSResponse]

      response.status throws new RuntimeException("Timeout error")
      requestHolder.get() returns Future.successful(response)
      httpLayer.url(any) returns requestHolder

      await(service.retrieveURL(email)) should beNone
    }

    "return secure Avatar url" in new Context {
      val requestHolder = mock[WSRequest]
      val response = mock[WSResponse]

      response.status returns 200
      requestHolder.get() returns Future.successful(response)
      httpLayer.url(any) returns requestHolder

      await(service.retrieveURL(email)) should beSome(SecureURL.format(hash, "?d=404"))
    }

    "return insecure Avatar url" in new Context {
      val requestHolder = mock[WSRequest]
      val response = mock[WSResponse]

      settings.secure returns false
      response.status returns 200
      requestHolder.get() returns Future.successful(response)
      httpLayer.url(any) returns requestHolder

      await(service.retrieveURL(email)) should beSome(InsecureURL.format(hash, "?d=404"))
    }

    "return an url with additional parameters" in new Context {
      val requestHolder = mock[WSRequest]
      val response = mock[WSResponse]

      settings.params returns Map("d" -> "http://example.com/images/avatar.jpg", "s" -> "400")
      response.status returns 200
      requestHolder.get() returns Future.successful(response)
      httpLayer.url(any) returns requestHolder

      await(service.retrieveURL(email)) should beSome(
        SecureURL.format(hash, "?d=http%3A%2F%2Fexample.com%2Fimages%2Favatar.jpg&s=400")
      )
    }

    "not trim leading zeros" in new Context {
      val requestHolder = mock[WSRequest]
      val response = mock[WSResponse]

      response.status returns 200
      requestHolder.get() returns Future.successful(response)
      httpLayer.url(any) returns requestHolder

      await(service.retrieveURL("123test@test.com")) should beSome(
        SecureURL.format("0d77aed6b4c5857473c9a04c2017f8b8", "?d=404")
      )
    }
  }

  /**
   * The context.
   */
  trait Context extends Scope {

    /**
     * The HTTP layer implementation.
     */
    val httpLayer = {
      val m = mock[HTTPLayer]
      m.executionContext returns defaultContext
      m
    }

    /**
     * The Gravatar service settings.
     */
    val settings = spy(GravatarServiceSettings())

    /**
     * The Gravatar service implementation.
     */
    val service = new GravatarService(httpLayer, settings)

    /**
     * The email for which the Avatar should be retrieved.
     */
    val email = "apollonia.vanova@watchmen.com"

    /**
     * The generated hash for the email address.
     *
     * @see http://en.gravatar.com/site/check/
     */
    val hash = "c6b0eb337880e2960f5bdbf0aa24c8ac"
  }
}
