/*
 * Made by Jakub Wierzbicki @jwierzb
 */

package com.jwierzbicki.rinkebyfaucet.http

import akka.actor.{Actor, ActorLogging, ActorRef, Props, ReceiveTimeout, Status}
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.{RequestContext, RouteResult}
import akka.http.scaladsl.model.StatusCodes._
import com.jwierzbicki.rinkebyfaucet.http.ActorPerRequest._
import com.jwierzbicki.rinkebyfaucet.model._

import scala.concurrent.Promise
import scala.concurrent.duration._

/*
  Actor's class for handling http requests performed by client.
  It's then routing requeset to child actor, named target.
  A target is specified by props arguments of contructor. It's implementation
  provides access to resources, eg to rest api or db.
  After that resources are routed back to the client.

 */
case class WithPropsActor(r: RequestContext, props: Props, message: ModelRequest, p: Promise[RouteResult]) extends ActorPerRequest {
  lazy val requestTarget: ActorRef = context.actorOf(props, "target")
}


trait ActorPerRequest extends Actor {

  import context._

  def r: RequestContext

  /**
    * Target actor which 'do the job'
    */
  def requestTarget: ActorRef

  def p: Promise[RouteResult]

  /**
    * message to target actor (requestTarget actor should know how to read it!)
    */
  def message: ModelRequest

  setReceiveTimeout(3.seconds)

  override def preStart(): Unit = {

    /*
      Handled from http controller requests are being send to the actors for further processing
      then actor's waiting for response
     */

    requestTarget ! message
  }

  override def receive: Receive = {
    case suc: SuccessfulTransaction => complete(OK, HttpEntity(ContentTypes.`text/html(UTF-8)`, s"<html><body><h>Transaction hash code: ${suc.hash}</h></body></html>"))
    case f: FailedTransaction => complete(NotFound, HttpEntity(ContentTypes.`text/html(UTF-8)`, s"<html><body><h>Internal error</h></body></html>"))
    case f: PublicKeyInvalid => complete(BadRequest, HttpEntity(ContentTypes.`text/html(UTF-8)`, s"<html><body><h>Invalid public key</h></body></html>"))
    case _ =>
      //Something bad has happened
      complete(BadRequest, HttpEntity(ContentTypes.`text/html(UTF-8)`, s"<html><body><h>Internal error</h></body></html>"))

  }

  def complete(m: => ToResponseMarshallable): Unit = {
    val f = r.complete(m)
    f.onComplete(p.complete(_))
    stop(self)
  }

}

object ActorPerRequest {


  /**
    * ActorPerRequest related domain model
    */
  trait ModelResponse

  final case class PublicKeyInvalid() extends ModelResponse

  final case class SuccessfulTransaction(hash: String) extends ModelResponse

  final case class FailedTransaction(message: String) extends ModelResponse

  final case class UnknownErrorR() extends ModelResponse

  final case class ConnectionFailedError() extends ModelResponse

}