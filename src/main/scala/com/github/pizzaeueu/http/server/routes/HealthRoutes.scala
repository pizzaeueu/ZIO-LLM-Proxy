package com.github.pizzaeueu.http.server.routes

import com.github.pizzaeueu.http.server.HttpServerLive.ApiV1Path
import zio.*
import zio.http.*

trait HealthRoutes:
  def build(): Routes[Any, Nothing]

final case class HealthRoutesLive() extends HealthRoutes:
  override def build(): Routes[Any, Nothing] = Routes(
    Method.GET / ApiV1Path / "health" / "check" -> handler(Response.ok)
  )

object HealthRoutesLive {
  val live: ULayer[HealthRoutes] = ZLayer.succeed(HealthRoutesLive())
}
