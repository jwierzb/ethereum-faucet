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
import com.jwierzbicki.rinkebyfaucet.actor.JsonRPCActor
import akka.http.scaladsl.model.StatusCodes._
import com.jwierzbicki.rinkebyfaucet.model.{ModelRequest, TransferRequest}

import scala.concurrent.{ExecutionContext, Promise}


trait ConnectionController {

  implicit def actorSystem: ActorSystem


  implicit def materializer: Materializer


  val mainRoute: Route =

    path("") {

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
      } ~
        post {
          formField("public_key") {
            pk => sendByPublicKey(TransferRequest(pk))
          }
        }

    } ~ complete(BadRequest, HttpEntity(ContentTypes.`text/html(UTF-8)`, "<html><body><h>404</h></body></html>"))

  def sendByPublicKey(request: TransferRequest): Route = {
    handleRequest(Props(classOf[JsonRPCActor], materializer), request)
  }

  def handleRequest(targetProps: Props, request: ModelRequest): Route = ctx => {
    val p = Promise[RouteResult]
    print(ctx)
    perRequest(ctx, targetProps, request, p)(actorSystem)
    p.future
  }

  def perRequest(r: RequestContext, props: Props, req: ModelRequest, p: Promise[RouteResult])(implicit ac: ActorSystem): ActorRef =
    ac.actorOf(Props(classOf[WithPropsActor], r, props, req, p), s"pr-${UUID.randomUUID().toString}")

}
