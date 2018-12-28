/*
 * Made by Jakub Wierzbicki @jwierzb
 */

package com.jwierzbicki.rinkebyfaucet.actor

import akka.actor.Props
import akka.stream.ActorMaterializer
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import com.sun.xml.internal.bind.v2.schemagen.xmlschema.Any
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem}


import akka.http.scaladsl.model._

import akka.stream.Materializer
import com.jwierzbicki.rinkebyfaucet.model.{JsonRPCError, JsonRPCFail, PublicKeyInvalid, TransferRequest}

import scala.concurrent.ExecutionContext


class MySpec() extends TestKit(ActorSystem("rinkeby-faucet")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "An SendTransactionActor" must {

    "reject invalid syntax's public key" in {
      implicit val materializer = ActorMaterializer()
      implicit val executionContext: ExecutionContext = system.dispatcher

      val actor: ActorRef = system.actorOf(Props(classOf[SendTransactionActor], system, materializer, executionContext))
      implicit var tran = TransferRequest("0xa2df2")

      actor ! tran
      expectMsg(PublicKeyInvalid)

    }
    "accept valid syntax's public key" in {

      implicit val materializer = ActorMaterializer()
      implicit val executionContext: ExecutionContext = system.dispatcher

      implicit var tran = TransferRequest("0x2f5c7f32666fcefd083a9e3c4fcb2d3f096089bf")
      implicit var error = JsonRPCError(-32000, "authentication needed: password or unlock")
      implicit val expectedResponse = JsonRPCFail("2.0", 1,error)

      val actor: ActorRef = system.actorOf(Props(classOf[SendTransactionActor], system, materializer, executionContext))

      actor ! tran
      var probe = TestProbe("test")

      expectMsg(expectedResponse)

    }
    "accept valid syntax HttpResponse" in {

      implicit val materializer = ActorMaterializer()
      implicit val executionContext: ExecutionContext = system.dispatcher

      val response: HttpResponse = HttpResponse(entity = HttpEntity.apply(ContentTypes.`application/json`, "{\"jsonrpc\":\"2.0\",\"id\":1,\"error\":{\"code\":-32000, \"message\":\"no suitable peers available\"}}"))

      implicit var error = JsonRPCError(-32000, "no suitable peers available")
      implicit val expectedResponse = JsonRPCFail("2.0", 1,error)

      val actor: ActorRef = system.actorOf(Props(classOf[HelperActor], system, materializer, executionContext), "sendert")

      var probe = TestProbe("test")

      actor ! probe.ref
      actor ! response

      probe.expectMsg(expectedResponse)

    }
  }
}

/**
  * Helper actor used to handle responses
 */
class HelperActor(implicit as: ActorSystem, ma: Materializer, ex: ExecutionContext) extends Actor
{

  import context._

  var target: ActorRef = context.actorOf(Props(classOf[SendTransactionActor], as, ma, ex), "target")
  var dest1: ActorRef = _

  override def receive: Receive =
  {
    case r: HttpResponse => {
      println("sendig to SendTransacionActor")
      target ! r
    }
    case r: JsonRPCFail => {
      println("sending to probe")
      print(r)
      dest1 ! r
    }
    case r: Any => sender() ! r
    case r: ActorRef => dest1 = r
  }

  def complete(r: Any):Unit={
    context.parent ! r
  }
}
