package com.github.pizzaeueu

import com.github.pizzaeueu.client.{LLMClientLive, ProxyMCPClientLive}
import com.github.pizzaeueu.http.server.routes.{HealthRoutesLive, ProxyRoutesLive}
import com.github.pizzaeueu.http.server.{HttpServer, HttpServerLive}
import com.github.pizzaeueu.services.*
import zio.*
import zio.config.typesafe.TypesafeConfigProvider
import zio.http.Server
import com.github.pizzaeueu.config.AppConfig
import com.github.pizzaeueu.service.PIICheckerLive

object Main extends ZIOAppDefault {

  override def run: ZIO[ZIOAppArgs & Scope, Any, Any] =
    ZIO
      .config[AppConfig]
      .tap(config => ZIO.log(s"Config: $config"))
      .flatMap { config =>
        ZIO
          .serviceWithZIO[HttpServer](_.start)
          .provide(
            HttpServerLive.live,
            Server.defaultWithPort(config.server.port),
            HealthRoutesLive.live,
            LLMClientLive.live,
            ProxyMCPClientLive.live,
            ZLayer.succeed(config.openai),
            ZLayer.succeed(config.mcp),
            UserRequestServiceLive.live,
            ProxyRoutesLive.live,
            PIICheckerLive.live,
            ClientStateRepositoryLive.live
          )
      }
      .withConfigProvider(TypesafeConfigProvider.fromResourcePath())
}
