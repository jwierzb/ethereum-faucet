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


object succesful
