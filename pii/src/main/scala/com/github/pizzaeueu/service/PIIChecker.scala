package com.github.pizzaeueu.service

import com.github.pizzaeueu.config.PiiConfig
import zio.*

trait PIIChecker {
  def containsSensitiveData(text: String): Task[Boolean]
}

final case class PIICheckerLive(piiConfig: PiiConfig) extends PIIChecker:
  override def containsSensitiveData(text: String): Task[Boolean] =
    ZIO.succeed(text.matches(piiConfig.regex))

object PIICheckerLive:
  def live = ZLayer.fromFunction(PIICheckerLive.apply)
