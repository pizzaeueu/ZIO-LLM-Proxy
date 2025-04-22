package com.github.pizzaeueu

import com.github.pizzaeueu.config.AppConfig
import com.github.pizzaeueu.http.server.routes.HealthRoutesLive
import com.github.pizzaeueu.http.server.{HttpServer, HttpServerLive}
import com.github.pizzaeueu.mcp.{ProxyMCPClient, ProxyMCPClientLive}
import zio.*
import zio.config.typesafe.TypesafeConfigProvider
import zio.http.Server

object Main extends ZIOAppDefault {

  override def run: ZIO[ZIOAppArgs & Scope, Any, Any] =
    ZIO
      .serviceWithZIO[ProxyMCPClient](_.listTools)
      .provide(ProxyMCPClientLive.live)
      .flatMap(tools => ZIO.log(tools)) *>
      ZIO
        .config[AppConfig]
        .tap(config => ZIO.log(s"Config: $config"))
        .flatMap { config =>
          ZIO
            .serviceWithZIO[HttpServer](_.start)
            .provide(
              HttpServerLive.live,
              Server.defaultWithPort(config.server.port),
              HealthRoutesLive.live
            )
        }
        .withConfigProvider(TypesafeConfigProvider.fromResourcePath())
}
