/*
 * Made by Jakub Wierzbicki @jwierzb
 */

package com.jwierzbicki.rinkebyfaucet.actor

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import com.sun.xml.internal.bind.v2.schemagen.xmlschema.Any
import akka.http.scaladsl.model._
import akka.stream.Materializer
import com.jwierzbicki.rinkebyfaucet.http.ActorPerRequest._
import com.jwierzbicki.rinkebyfaucet.model._

import scala.concurrent.ExecutionContext


class JsonRPCActorTest extends TestKit(ActorSystem("rinkeby-faucet")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {


  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "An SendTransactionActor" must {

    "reject invalid syntax's public key" in {
      implicit val materializer = ActorMaterializer()
      implicit val executionContext: ExecutionContext = system.dispatcher

      val actor: ActorRef = system.actorOf(Props(classOf[JsonRPCActor], materializer))
      implicit var tran = TransferRequest("0xa2df2")

      actor ! tran
      expectMsg(PublicKeyInvalid())

    }

    "accept valid syntax HttpResponse with failure" in {

      implicit val materializer = ActorMaterializer()
      implicit val executionContext: ExecutionContext = system.dispatcher

      val response: HttpResponse = HttpResponse(entity = HttpEntity.apply(ContentTypes.`application/json`, "{\"jsonrpc\":\"2.0\",\"id\":1,\"error\":{\"code\":-32000, \"message\":\"no suitable peers available\"}}"))

      val actor: ActorRef = system.actorOf(Props(classOf[HelperActor], materializer), "sender-1")

      var probe = TestProbe("probe-1")

      actor ! probe.ref
      actor ! response

      probe.expectMsg(FailedTransaction("no suitable peers available"))

    }
    "accept valid syntax HttpResponse with succes" in {

      implicit val materializer = ActorMaterializer()
      implicit val executionContext: ExecutionContext = system.dispatcher

      val response: HttpResponse = HttpResponse(entity = HttpEntity.apply(ContentTypes.`application/json`, "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":\"0x2test\"}"))

      val actor: ActorRef = system.actorOf(Props(classOf[HelperActor], materializer), "sender-2")

      var probe = TestProbe("probe-2")

      actor ! probe.ref
      actor ! response

      probe.expectMsg(SuccessfulTransaction("0x2test"))

    }
    "should reached timeout due to unmarshaling imposibility of unmatching json in response body" in {

      implicit val materializer = ActorMaterializer()
      implicit val executionContext: ExecutionContext = system.dispatcher

      val response: HttpResponse = HttpResponse(entity = HttpEntity.apply(ContentTypes.`application/json`, "{\"jsonrpc\":\"2.0\"\"}"))

      val actor: ActorRef = system.actorOf(Props(classOf[HelperActor], materializer), "sender-3")

      var probe = TestProbe("probe-3")

      actor ! probe.ref
      actor ! response

      import scala.concurrent.duration._
      probe.expectNoMessage(1.second)

    }
  }



}
/**
  * Helper actor used to handle responses
  */
class HelperActor(implicit ma: Materializer) extends Actor with ActorLogging
{

  import context._

  var target: ActorRef = context.actorOf(Props(classOf[JsonRPCActor], ma), "target")
  var dest1: ActorRef = _


  override def receive: Receive =
  {
    case r: HttpResponse => {
      target ! r
    }
    case r: FailedTransaction => {
      dest1 ! r
    }
    case r: SuccessfulTransaction => {
      dest1 ! r
    }
    case r: Any => sender() ! r
    case r: ActorRef => dest1 = r
  }
}