/*
 * Made by Jakub Wierzbicki @jwierzb
 */

package com.jwierzbicki.rinkebyfaucet.http

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.ExecutionContext

class WebServer(config: Config) (implicit as: ActorSystem, mat: Materializer, ec: ExecutionContext) extends FaucetRoute {

  override def actorSystem: ActorSystem = implicitly
  override def executioner: ExecutionContext = implicitly
  override def materializer: Materializer = implicitly


  val route: Route = mainRoute

  def start():Unit={
    implicit val addres = config.getString("http.routing.addres")
    implicit val port = config.getInt("http.routing.port")

    Http().bind(addres, port).runForeach(_.handleWith(Route.handlerFlow(route)))
    println(s"Server started on addres ${addres} on port ${port}")
  }

}
