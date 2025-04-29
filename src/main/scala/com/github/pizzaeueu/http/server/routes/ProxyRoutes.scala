package com.github.pizzaeueu.http.server.routes

import zio.http.*
import com.github.pizzaeueu.http.server.HttpServerLive.ApiV1Path
import zio.*
import zio.json.*
import com.github.pizzaeueu.domain.*
import com.github.pizzaeueu.services.UserRequestService

trait ProxyRoutes:
  def build(): Routes[Any, Nothing]

final case class ProxyRoutesLive(userRequestService: UserRequestService)
    extends ProxyRoutes:
  private implicit val userPromptDecoder: JsonDecoder[UserPrompt] =
    DeriveJsonDecoder.gen[UserPrompt]
  override def build(): Routes[Any, Nothing] = Routes(
    Method.POST / ApiV1Path / "model" / "ask" -> handler { (request: Request) =>
      for {
        bodyStr <- request.body.asString
        userPrompt <- ZIO.fromEither(
          bodyStr
            .fromJson[UserPrompt]
            .left
            .map(err => new RuntimeException(err))
        )
        modelResponse <- userRequestService.ask(userPrompt.text)
      } yield Response.text(modelResponse)
    }
  ).handleError(err => Response.internalServerError(err.getMessage))

object ProxyRoutesLive:
  val live= ZLayer.fromFunction(ProxyRoutesLive.apply)
