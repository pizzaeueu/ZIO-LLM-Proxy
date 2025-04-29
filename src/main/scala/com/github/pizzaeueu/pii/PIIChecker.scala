package com.github.pizzaeueu.pii

import zio.*

trait PIIChecker {
  def containsSensitiveData(text: String): Task[Boolean]
}

final case class PIICheckerLive() extends PIIChecker:
  override def containsSensitiveData(text: String): Task[Boolean] =
    ZIO.succeed(true)

object PIICheckerLive:
  def live = ZLayer.succeed(PIICheckerLive())
