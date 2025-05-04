package com.github.pizzaeueu

import com.github.pizzaeueu.client.{LLMClientLive, ProxyMCPClientLive}
import com.github.pizzaeueu.config.AppConfig
import com.github.pizzaeueu.http.server.routes.{
  HealthRoutesLive,
  ProxyRoutesLive
}
import com.github.pizzaeueu.http.server.{HttpServer, HttpServerLive}
import com.github.pizzaeueu.service.PIICheckerLive
import com.github.pizzaeueu.services.*
import zio.*
import zio.http.Server

object Main extends ZIOAppDefault {

  override def run: ZIO[ZIOAppArgs & Scope, Any, Any] =

    AppConfig.loadConfigs()
      .tap { case (appConfig, mcpConfig) => ZIO.log(s"Config: $appConfig, $mcpConfig") }
      .flatMap { (config, mcpConfig) =>
        ZIO
          .serviceWithZIO[HttpServer](_.start)
          .provide(
            HttpServerLive.live,
            Server.defaultWithPort(config.server.port),
            HealthRoutesLive.live,
            LLMClientLive.live,
            ProxyMCPClientLive.live,
            ZLayer.succeed(config.openai),
            ZLayer.succeed(mcpConfig.data),
            UserRequestServiceLive.live,
            ProxyRoutesLive.live,
            PIICheckerLive.live,
            ClientStateRepositoryLive.live,
            ZLayer.succeed(config.pii)
          )
      }
      //.withConfigProvider(TypesafeConfigProvider.fromResourcePath())
}
