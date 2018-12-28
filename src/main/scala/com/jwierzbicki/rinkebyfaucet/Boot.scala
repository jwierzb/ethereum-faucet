import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import com.jwierzbicki.rinkebyfaucet.http.{FaucetRoute, WebServer}

import scala.concurrent.{Await, ExecutionContext, Future}
import com.typesafe.config._

import scala.io.StdIn
import scala.concurrent.duration._


object Boot extends App {

  def startApplication(): Unit =
  {

    implicit val system = ActorSystem("rinkeby-faucet", ConfigFactory.load())
    implicit val materializer = ActorMaterializer()
    implicit val executionContext: ExecutionContext = system.dispatcher

    new WebServer(ConfigFactory.load()).start()

    sys.addShutdownHook(() => {
      val future = system.terminate()
      Await.result(future, 120.seconds)
    })
  }

  startApplication()
}