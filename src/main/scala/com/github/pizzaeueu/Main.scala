package com.github.pizzaeueu

import com.github.pizzaeueu.config.AppConfig
import com.github.pizzaeueu.http.server.routes.HealthRoutesLive
import com.github.pizzaeueu.http.server.{HttpServer, HttpServerLive}
import com.github.pizzaeueu.llm.{LLMClient, LLMClientLive}
import com.github.pizzaeueu.mcp.{ProxyMCPClient, ProxyMCPClientLive}
import zio.*
import zio.config.typesafe.TypesafeConfigProvider
import zio.http.Server

object Main extends ZIOAppDefault {

  override def run: ZIO[ZIOAppArgs & Scope, Any, Any] =
    ZIO
      .config[AppConfig]
      .tap(config => ZIO.log(s"Config: $config"))
      .flatMap { config =>
        (ZIO.serviceWithZIO[LLMClient](
          _.sendRequest("Which files I have in my system?")
        ) *>
          ZIO
            .serviceWithZIO[HttpServer](_.start))
          .provide(
            HttpServerLive.live,
            Server.defaultWithPort(config.server.port),
            HealthRoutesLive.live,
            LLMClientLive.live,
            ProxyMCPClientLive.live,
            ZLayer.succeed(config.openai),
            ZLayer.succeed(config.mcp),
          )
      }
      .withConfigProvider(TypesafeConfigProvider.fromResourcePath())
}
