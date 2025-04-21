package com.github.pizzaeueu

import com.github.pizzaeueu.http.server.routes.HealthRoutesLive
import com.github.pizzaeueu.http.server.{HttpServer, HttpServerLive}
import zio.*
import zio.http.Server

object Main extends ZIOAppDefault {

  override def run: ZIO[ZIOAppArgs & Scope, Any, Any] =
    ZIO.
      serviceWithZIO[HttpServer](_.start)
      .provide(
        HttpServerLive.live,
        Server.default,
        HealthRoutesLive.live
      )
}
