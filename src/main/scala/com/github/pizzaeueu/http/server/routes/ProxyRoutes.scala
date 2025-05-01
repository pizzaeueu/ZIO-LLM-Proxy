package com.github.pizzaeueu.http.server.routes

import zio.http.*
import com.github.pizzaeueu.http.server.HttpServerLive.ApiV1Path
import zio.*
import zio.json.*
import com.github.pizzaeueu.domain.*
import com.github.pizzaeueu.domain.RequestState.*
import com.github.pizzaeueu.domain.llm.{Dialogue, LLMResponse}
import com.github.pizzaeueu.repository.ClientStateRepository
import com.github.pizzaeueu.services.UserRequestService
import zio.http.ChannelEvent.*

trait ProxyRoutes:
  def build(): Routes[Any, Nothing]

final case class ProxyRoutesLive(
    userRequestService: UserRequestService,
    stateRepository: ClientStateRepository
) extends ProxyRoutes:
  private implicit val userPromptDecoder: JsonDecoder[UserPrompt] =
    DeriveJsonDecoder.gen[UserPrompt]
  private implicit val UserAllowDecoder: JsonDecoder[UserAllow] =
    DeriveJsonDecoder.gen[UserAllow]

  val socketApp: WebSocketApp[Any] =
    Handler.webSocket { channel =>
      for {
        clientId <- Random.nextUUID
        _ <- stateRepository.saveState(clientId.toString, Started)
        _ <- channel.receiveAll {
          case Read(WebSocketFrame.Text(text)) =>
            (for {
              stateMayBe <- stateRepository.getState(clientId.toString)
              state <- ZIO
                .fromOption(stateMayBe)
                .mapError(_ => new RuntimeException("State not found"))
              _ <- state match {
                case RequestState.WaitingForApprove(dialogue) =>
                  for {
                    userAllow <- ZIO.fromEither(
                      text
                        .fromJson[UserAllow]
                        .left
                        .map(err => new RuntimeException(err))
                    )
                    _ <- {
                      if (userAllow.allow) {
                        userRequestService
                          .ask(
                            dialogue.copy(secure = false)
                          )
                          .flatMap(res =>
                            channel.send(
                              Read(WebSocketFrame.Text(res.toString))
                            )
                          )
                      } else {
                        channel.send(
                          Read(WebSocketFrame.Text("Connection is closed"))
                        )
                      }
                    }
                  } yield ()
                case _ =>
                  for {
                    userPrompt <- ZIO.fromEither(
                      text
                        .fromJson[UserPrompt]
                        .left
                        .map(err => new RuntimeException(err))
                    )
                    modelResponse <- userRequestService.ask(
                      Dialogue.startSecure(clientId.toString, userPrompt)
                    )
                    _ <- modelResponse match {
                      case LLMResponse.Success(text) =>
                        channel.send(
                          Read(WebSocketFrame.Text(text))
                        )
                      case LLMResponse.SensitiveDataFound =>
                        channel.send(
                          Read(
                            WebSocketFrame.Text(
                              "Sensitive data found. Please approve the request."
                            )
                          )
                        )
                    }
                  } yield ()
              }
            } yield ()).catchAll(err =>
              ZIO.logError(err.getMessage) *> channel.send(
                Read(WebSocketFrame.Text(err.getMessage))
              )
            )
          case other =>
            ZIO
              .logError(s"Received unknown message: $other")
              .as(())
        }
      } yield ()
    }
  override def build(): Routes[Any, Nothing] = Routes(
    Method.GET / ApiV1Path / "model" / "ask" -> handler(socketApp.toResponse)
  )

object ProxyRoutesLive:
  val live = ZLayer.fromFunction(ProxyRoutesLive.apply)
