import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.jwierzbicki.rinkebyfaucet.http.{ WebServer}

import scala.concurrent.{Await, ExecutionContext}
import com.typesafe.config._

import scala.concurrent.duration._


object Boot extends App {

  def startApplication(): Unit = {

    implicit val system = ActorSystem("rinkeby-faucet", ConfigFactory.load())
    implicit val materializer = ActorMaterializer()
    implicit val executionContext: ExecutionContext = system.dispatcher

    // start the party
    new WebServer(ConfigFactory.load()).start()

    sys.addShutdownHook(() => {
      val future = system.terminate()
      Await.result(future, 120.seconds)
    })
  }

  startApplication()
}