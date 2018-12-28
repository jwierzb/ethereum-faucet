/*
 * Made by Jakub Wierzbicki @jwierzb
 */

package com.jwierzbicki.rinkebyfaucet.http

import akka.actor.{Actor, ActorLogging, ActorRef, Props, ReceiveTimeout, Status}
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.{RequestContext, RouteResult}
import akka.http.scaladsl.model.StatusCodes._
import com.jwierzbicki.rinkebyfaucet.model.{PublicKeyInvalid, Request, SuccessfulTransaction}

import scala.concurrent.Promise
import scala.concurrent.duration._

/*
  Actor's class for handling http requests performed by client.
  It's then routing requeset to child actor, named target.
  A target is specified by props arguments of contructor. It's implementation
  provides access to resources. Then resources are routed to the client.

 */

case class WithPropsActor(r: RequestContext, props: Props, message: Request, p: Promise[RouteResult]) extends ActorPerRequest {
  lazy val requestTarget: ActorRef = context.actorOf(props, "target")
}
trait ActorPerRequest extends Actor with ActorLogging{

  import context._

  def r: RequestContext
  def requestTarget: ActorRef
  def p: Promise[RouteResult]
  def message: Request

  setReceiveTimeout(3.seconds)

  override def preStart(): Unit = {

    /*
      Handled from http controller requests are being send to the actors for further processing
      then actor's waiting for response
     */

    requestTarget ! message
  }

  override def receive: Receive = {
      case suc: SuccessfulTransaction => complete(OK, HttpEntity(ContentTypes.`text/html(UTF-8)`,s"<html><body><h>Transaction hash code: ${suc.hash}</h></body></html>"))
      case PublicKeyInvalid => complete(BadRequest, HttpEntity(ContentTypes.`text/html(UTF-8)`,s"<html><body><h>Invalid public key</h></body></html>"))

      case _ => {
        //Something bad has happened
        complete(BadRequest, HttpEntity(ContentTypes.`text/html(UTF-8)`,s"<html><body><h>Internal error</h></body></html>"))
      }
  }

  def complete(m: => ToResponseMarshallable): Unit = {
    val f = r.complete(m)
    f.onComplete(p.complete(_))
    stop(self)
  }

}




