/*
 * Made by Jakub Wierzbicki @jwierzb
 */

package com.jwierzbicki.rinkebyfaucet.http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import com.typesafe.config.Config

import scala.io.StdIn


/**
  * Provider of the http server existance
  */
class WebServer(config: Config)(implicit as: ActorSystem, mat: Materializer) extends ConnectionController {

  override def actorSystem: ActorSystem = implicitly

  override def materializer: Materializer = implicitly

  implicit val executionContext = actorSystem.dispatcher



  def start(): Unit = {
    val addres = config.getString("http.routing.addres")
    val port = config.getInt("http.routing.port")

    //Bind every connections with ConnectionController
    val future = Http().bindAndHandle(mainRoute, addres, port)

    future.onComplete({
      case scala.util.Success(value)  =>{
        println(s"Server started on addres ${addres} on port ${port}")
        actorSystem.log.info(s"Server started on addres ${addres} on port ${port}")
      }
      case scala.util.Failure(exception) => {
        actorSystem.log.error(exception, s"Failed to bind to ${addres}:${port}!")
        println("Server failed to bind")
      }
    })

    println("Press enter to terminate")
    StdIn.readLine()

    future
      .flatMap(_.unbind())
      .onComplete(_ => actorSystem.terminate())
  }

}
