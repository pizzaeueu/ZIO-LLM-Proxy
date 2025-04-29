package com.github.pizzaeueu.http.server

import com.github.pizzaeueu.http.server.routes.{HealthRoutes, ProxyRoutes}
import zio.http.{Root, Server}
import zio.{URIO, ZLayer}

trait HttpServer {
  def start: URIO[Server, Nothing]
}

case class HttpServerLive(healthRoutes: HealthRoutes, proxyRoutes: ProxyRoutes)
    extends HttpServer {

  override def start: URIO[Server, Nothing] =
    Server.serve(healthRoutes.build() ++ proxyRoutes.build())
}

object HttpServerLive {
  private[server] val ApiV1Path = Root / "api" / "v1"
  def live: ZLayer[HealthRoutes & ProxyRoutes, Nothing, HttpServerLive] =
    ZLayer.fromFunction(HttpServerLive.apply)
}

