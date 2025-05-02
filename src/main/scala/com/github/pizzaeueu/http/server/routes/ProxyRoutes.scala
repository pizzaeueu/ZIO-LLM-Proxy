package com.github.pizzaeueu.http.server.routes

import com.github.pizzaeueu.domain
import com.github.pizzaeueu.domain.*
import com.github.pizzaeueu.domain.RequestState.*
import com.github.pizzaeueu.domain.llm.{Dialogue, LLMResponse}
import com.github.pizzaeueu.http.server.HttpServerLive.ApiV1Path
import com.github.pizzaeueu.repository.ClientStateRepository
import com.github.pizzaeueu.services.UserRequestService
import zio.*
import zio.http.*
import zio.http.ChannelEvent.*
import zio.json.*
import zio.json.internal.Write

trait ProxyRoutes:
  def build(): Routes[Any, Nothing]

final case class ProxyRoutesLive(
    userRequestService: UserRequestService,
    stateRepository: ClientStateRepository
) extends ProxyRoutes:
  private given JsonDecoder[UserPrompt] =
    DeriveJsonDecoder.gen[UserPrompt]
  private given JsonDecoder[UserAllow] =
    DeriveJsonDecoder.gen[UserAllow]

  private given JsonEncoder[LLMResponse] =
    (a: LLMResponse, indent: Option[RuntimeFlags], out: Write) =>
      a match {
        case domain.llm.LLMResponse.Success(text) =>
          out.write(s"""{"type": "success", "text": "$text" + }""")
        case domain.llm.LLMResponse.SensitiveDataFound =>
          out.write(
            s"""{"type": "sensitive-info", "text": "Please approve the request." + }"""
          )
      }

  private given JsonEncoder[Throwable] =
    (a: Throwable, indent: Option[RuntimeFlags], out: Write) =>
      out.write(
        s"""{"type": "error", "text": "${a.getMessage}" + }"""
      )

  val socketApp: WebSocketApp[Any] =
    Handler.webSocket { channel =>
      for {
        clientId <- Random.nextUUID
        _ <- stateRepository.saveState(clientId.toString, InProgress)
        _ <- channel.receiveAll {
          case Read(WebSocketFrame.Text(text)) =>
            (for {
              stateMayBe <- stateRepository.getState(clientId.toString)
              state <- ZIO
                .fromOption(stateMayBe)
                .mapError(_ => StateNotFound)
              _ <- state match {
                case RequestState.WaitingForApprove(dialogue) =>
                  for {
                    userAllow <- ZIO.fromEither(
                      text
                        .fromJson[UserAllow]
                        .left
                        .map(err => ParsingError(err))
                    )
                    _ <- {
                      if (userAllow.allow) {
                        userRequestService
                          .ask(
                            dialogue.copy(secure = false)
                          )
                          .flatMap(res =>
                            channel.send(
                              Read(WebSocketFrame.Text(res.toJson))
                            )
                          )
                      } else {
                        channel.shutdown
                      }
                    }
                  } yield ()
                case _ =>
                  for {
                    userPrompt <- ZIO.fromEither(
                      text
                        .fromJson[UserPrompt]
                        .left
                        .map(err => ParsingError(err))
                    )
                    modelResponse <- userRequestService.ask(
                      Dialogue.startSecure(clientId.toString, userPrompt)
                    )
                    _ <- channel.send(
                      Read(WebSocketFrame.Text(modelResponse.toJson))
                    )
                  } yield ()
              }
            } yield ()).catchAll(err =>
              ZIO.logError(err.getMessage) *> channel.send(
                Read(WebSocketFrame.Text(err.toJson))
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
