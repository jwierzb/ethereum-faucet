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
trait ModelRequest

final case class TransferRequest(key: String) extends ModelRequest
final case class NullRequest(int: Int) extends ModelRequest

