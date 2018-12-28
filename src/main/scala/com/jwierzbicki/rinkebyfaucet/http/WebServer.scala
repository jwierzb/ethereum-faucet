/*
 * Made by Jakub Wierzbicki @jwierzb
 */

package com.jwierzbicki.rinkebyfaucet.http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import com.typesafe.config.Config


/**
  * Provider of the http server existance
  */
class WebServer(config: Config)(implicit as: ActorSystem, mat: Materializer) extends ConnectionController {

  override def actorSystem: ActorSystem = implicitly

  override def materializer: Materializer = implicitly


  val route: Route = mainRoute

  def start(): Unit = {
    val addres = config.getString("http.routing.addres")
    val port = config.getInt("http.routing.port")

    //Bind every connections with ConnectionController
    Http().bind(addres, port).runForeach(_.handleWith(Route.handlerFlow(route)))
    println(s"Server started on addres ${addres} on port ${port}")
  }

}
