/*
 * Made by Jakub Wierzbicki @jwierzb
 */

/*
 * Made by Jakub Wierzbicki @jwierzb
 */

package com.jwierzbicki.rinkebyfaucet.model


/**
  * Http controller domain model
  */
trait Request
final case class TransferRequest(key: String) extends Request



/**
  * ActorPerRequest related domain model
  */
trait ResponseComm
final case class PublicKeyInvalid() extends ResponseComm
final case class SuccessfulTransaction(hash: String) extends ResponseComm
final case class UnknownErrorR() extends ResponseComm
final case class ConnectionFailedError() extends ResponseComm


/**
  * JsonRPC related domain model
  */
final case class JsonRPCMethod(jsonrpc: String, method: String, params: List[ String], id: Int)
final case class JsonRPCSendCoin(jsonrpc: String, method: String, params: List[ Map[String, String]], id: Int)

final case class JsonRPCError(code: Int, message: String)
final case class JsonRPCSucces( jsonrpc: String, id: Int, result: String)
final case class JsonRPCFail(jsonrpc: String ,id: Int, error: JsonRPCError)
