/*
 * Made by Jakub Wierzbicki @jwierzb
 */

package com.jwierzbicki.rinkebyfaucet.actor


import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Status}
import com.jwierzbicki.rinkebyfaucet.Json.JsonSupport
import com.typesafe.config.ConfigFactory
import spray.json._

import scala.concurrent.Await
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._

import scala.concurrent.duration._
import akka.stream.Materializer
import com.jwierzbicki.rinkebyfaucet.http.ActorPerRequest._
import com.jwierzbicki.rinkebyfaucet.model._


/**
  * An actor class receiving requests from his parent, send request to JsonRPC api and then respond with right message
  */

class JsonRPCActor(implicit ma: Materializer) extends Actor with ActorLogging with JsonSupport {

  import context._
  import context.dispatcher
  import com.jwierzbicki.rinkebyfaucet.actor.JsonRPCActor._

  /**
    * Fields important in Future processing (like processing HttpResponse Future or with json marshalling)
    */
  setReceiveTimeout(1.seconds)


  override def receive: Receive = {

    /**
      * Receive request from parent actor
      */
    case tr: TransferRequest => {
      /**
        * Pre-processing
        */
      val pattern = "0x[0-9a-fA-F]{40}".r
      val string = pattern.replaceFirstIn(tr.key, "")

      if (tr.key.isEmpty || !string.isEmpty || tr.key == null) {
        sender() ! PublicKeyInvalid()
      }
      else {
        val configRinkeby = ConfigFactory.load()
        /*
        build request to JsonRPC api
         */
        val params =
          List(Map(
            "from" -> configRinkeby.getString("rinkeby.account.public-key"),
            "to" -> tr.key,
            "gas" -> "0x".concat(configRinkeby.getInt("rinkeby.gas-limit").toHexString),
            "gasPrice" -> "0x".concat(configRinkeby.getInt("rinkeby.gas-price").toHexString),
            "value" -> "0x".concat(configRinkeby.getInt("rinkeby.value").toHexString),
            "data" -> "0x0a"))

        val req = JsonRPCSendCoin("2.0", "eth_sendTransaction", params, 1)
        log.debug(s"Attemption to send Ether to ${tr.key}")
        val request = HttpRequest(
          method = HttpMethods.POST,
          uri = "http://" + configRinkeby.getString("http.connecting.addres") + ":" + configRinkeby.getString("http.connecting.port"),
          entity = HttpEntity(ContentTypes.`application/json`, req.toJson.toString())
        )

        import akka.pattern.pipe

        /**
          * Send Http request and pipe result to self for further processing
          */
        Http(context.system).singleRequest(request) pipeTo self
      }
    }


    /**
      * Receive httpresponse from self and process it
      */
    case res: HttpResponse => {

      /*
        JsonRPC returns one of two types responses, different when error and when successed
        to execute command. Most offen it should be success. Possible errors when e.g. wallet
        is locked or out of ether.
        Try to unmarshal response in right way assuming response is one of two types
        if response doesnt match any of tries-catch, send error message back to parent
       */

      import akka.http.scaladsl.unmarshalling.Unmarshal
      try {
        val response = Await.result(Unmarshal(res.entity).to[JsonRPCSucces], 10.millis)
        context.parent ! SuccessfulTransaction(response.result)
      }
      catch {
        case ex: DeserializationException => {
          try {
            val response = Await.result(Unmarshal(res.entity).to[JsonRPCFail], 10.millis)
              context.parent ! FailedTransaction(response.error.message)
              log.error(s"JsonRPC signals error: ${response.error.message}")
          } catch {
            case ex: Exception => {
              context.parent ! UnknownErrorR
              log.error(s"Unknown error: ${res.toString()}")
            }
          }
        }
        case ex: Exception => {
          context.parent ! UnknownErrorR
          log.error(s"Unknown error: ${res.toString()}")
        }

      }
    }
    case s: Status.Failure => {
      log.error(s"Testnet server error: ${s}")

      context.parent ! UnknownErrorR
    }

  }
}
object JsonRPCActor {


  /**
    * JsonRPC related domain model
    */
  final case class JsonRPCMethod(jsonrpc: String, method: String, params: List[String], id: Int)

  final case class JsonRPCSendCoin(jsonrpc: String, method: String, params: List[Map[String, String]], id: Int)

  final case class JsonRPCError(code: Int, message: String)

  final case class JsonRPCSucces(jsonrpc: String, id: Int, result: String)

  final case class JsonRPCFail(jsonrpc: String, id: Int, error: JsonRPCError)

}