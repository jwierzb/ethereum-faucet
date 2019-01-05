/*
 * Made by Jakub Wierzbicki @jwierzb
 */

package com.jwierzbicki.rinkebyfaucet.http

import java.util.UUID

import akka.http.scaladsl.server.Directives.{pathPrefix, _}
import akka.actor.{ActorSystem, Props}
import akka.http.impl.engine.client.PoolFlow
import akka.http.scaladsl.server.{RequestContext, Route, RouteResult}
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.RouteResult.Complete
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.{ActorMaterializer, Materializer}
import akka.stream.actor.ActorPublisherMessage.Request
import akka.testkit.{ImplicitSender, TestKit}
import com.jwierzbicki.rinkebyfaucet.actor.JsonRPCActor
import com.jwierzbicki.rinkebyfaucet.model._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.{Matchers, WordSpec}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.server._
import Directives._

import scala.concurrent.Promise

class ActorPerRequestTest() extends WordSpec
   with Matchers with ScalatestRouteTest{

  val message1  = SuccessfulTransaction("hash")
  val message2  = FailedTransaction("")
  val message3  = PublicKeyInvalid()

  def route(mess: ModelResponse): Route = {
    post {ctx => {
    val p = Promise[RouteResult]
    val actorRef = system.actorOf (Props (classOf[WithPropsActor], ctx, Props (classOf[JsonRPCActor], materializer), NullRequest (1), p) )
    actorRef ! mess
    p.future
  }}
  }


  "PerRequest actor" must{
    "give right transaction hash" in{
      Post() ~> route(message1)~> check{
        responseAs[String] shouldEqual s"<html><body><h>Transaction hash code: ${"hash"}</h></body></html>"
      }
    }
    "give internal error" in{
      Post() ~> route(message2) ~> check{
        responseAs[String] shouldEqual s"<html><body><h>Internal error</h></body></html>"
      }
    }
    "signal invalid key" in{
      Post() ~> route(message3) ~> check{
        responseAs[String] shouldEqual s"<html><body><h>Invalid public key</h></body></html>"
      }
    }
    "should give internal error" in{
      Post() ~> route(message2) ~> check{
        responseAs[String] shouldEqual s"<html><body><h>Invalid public key</h></body></html>"
      }
    }


  }

}
