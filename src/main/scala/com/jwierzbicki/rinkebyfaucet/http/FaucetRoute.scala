/*
 * Made by Jakub Wierzbicki @jwierzb
 */


package com.jwierzbicki.rinkebyfaucet.http

import java.util.UUID

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.{Directives, RequestContext, Route, RouteResult}
import akka.http.scaladsl.server.Directives.{pathPrefix, _}
import akka.stream.Materializer
import com.jwierzbicki.rinkebyfaucet.actor.SendTransactionActor
import akka.http.scaladsl.model.StatusCodes._
import com.jwierzbicki.rinkebyfaucet.model.TransferRequest

import scala.concurrent.{ExecutionContext, Promise}


trait FaucetRoute {

  implicit def actorSystem: ActorSystem
  implicit def executioner: ExecutionContext
  implicit def materializer: Materializer


  val mainRoute: Route =

    path(""){

        get {
          complete(HttpEntity(
            ContentTypes.`text/html(UTF-8)`,
            """
                  <form method="post" action="" name="form" id="id2">
                    <input type="text" name="public_key">
                    <input type="submit" value="Get Ether">
                  </form>
                """
          ))
        }~
          post {
              formField("public_key")
              {
                  pk=>
                sendByPublicKey(TransferRequest(pk))

            }


          }

    }~complete(BadRequest, HttpEntity(ContentTypes.`text/html(UTF-8)`,"<html><body><h>404</h></body></html>"))


  def perRequest(r: RequestContext, props: Props, req: TransferRequest, p: Promise[RouteResult]) (implicit ac: ActorSystem): ActorRef =
    ac.actorOf(Props(classOf[WithPropsActor], r, props, req, p), s"pr-${UUID.randomUUID().toString}")

  def handleRequest(targetProps: Props, request: TransferRequest): Route = ctx => {
    val p = Promise[RouteResult]
    perRequest(ctx, targetProps, request, p)(actorSystem)
    p.future
  }

  def sendByPublicKey(request: TransferRequest): Route =
  {
    handleRequest(Props(classOf[SendTransactionActor], actorSystem, materializer, executioner), request)
  }

}
