package com.github.pizzaeueu.http.server.routes

import zio.http.*
import com.github.pizzaeueu.http.server.HttpServerLive.ApiV1Path
import zio.*
import zio.json.*
import com.github.pizzaeueu.domain.*
import com.github.pizzaeueu.services.UserRequestService
import zio.http.ChannelEvent.*

trait ProxyRoutes:
  def build(): Routes[Any, Nothing]

final case class ProxyRoutesLive(userRequestService: UserRequestService)
    extends ProxyRoutes:
  private implicit val userPromptDecoder: JsonDecoder[UserPrompt] =
    DeriveJsonDecoder.gen[UserPrompt]

  val socketApp: WebSocketApp[Any] =
    Handler.webSocket { channel =>
      channel.receiveAll {
        case Read(WebSocketFrame.Text(text)) =>
          (for {
            _ <- ZIO.logInfo(s"Received message: $text")
            userPrompt <- ZIO.fromEither(
              text
                .fromJson[UserPrompt]
                .left
                .map(err => new RuntimeException(err))
            )
            modelResponse <- userRequestService.ask(userPrompt.text)
            _ <- channel.send(Read(WebSocketFrame.Text(modelResponse)))
          } yield ()).catchAll(err =>
            ZIO.logError(err.getMessage) *> channel.send(
              (Read(WebSocketFrame.Text(err.getMessage)))
            )
          )
        case other =>
          ZIO
            .logError(s"Received unknown message: $other")
            .as(())
      }
    }
  override def build(): Routes[Any, Nothing] = Routes(
    Method.GET / ApiV1Path / "model" / "ask" -> handler(socketApp.toResponse)
  )

object ProxyRoutesLive:
  val live = ZLayer.fromFunction(ProxyRoutesLive.apply)
