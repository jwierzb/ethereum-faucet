/*
 * Made by Jakub Wierzbicki @jwierzb
 */

/*
 * Made by Jakub Wierzbicki @jwierzb
 */

package com.jwierzbicki.rinkebyfaucet.model

/*
  Classes uses in communication between actors
 */

/**
  * Http controller domain model
  */
trait ModelRequest

final case class TransferRequest(key: String) extends ModelRequest
final case class NullRequest(int: Int) extends ModelRequest

/**
  * ActorPerRequest related domain model
  */
trait ModelResponse

final case class PublicKeyInvalid() extends ModelResponse

final case class SuccessfulTransaction(hash: String) extends ModelResponse

final case class FailedTransaction(message: String) extends ModelResponse

final case class UnknownErrorR() extends ModelResponse

final case class ConnectionFailedError() extends ModelResponse

object succesful
/**
  * JsonRPC related domain model
  */
final case class JsonRPCMethod(jsonrpc: String, method: String, params: List[String], id: Int)

final case class JsonRPCSendCoin(jsonrpc: String, method: String, params: List[Map[String, String]], id: Int)

final case class JsonRPCError(code: Int, message: String)

final case class JsonRPCSucces(jsonrpc: String, id: Int, result: String)

final case class JsonRPCFail(jsonrpc: String, id: Int, error: JsonRPCError)
