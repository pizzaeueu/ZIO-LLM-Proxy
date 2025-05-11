package com.github.pizzaeueu.http.server.routes

import com.github.pizzaeueu.domain
import com.github.pizzaeueu.domain.*
import com.github.pizzaeueu.domain.RequestState.{InProgress, WaitingForApprove}
import com.github.pizzaeueu.http.server.HttpServerLive.ApiV1Path
import com.github.pizzaeueu.services.{ClientStateRepository, UserRequestService}
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
        case LLMResponse.Success(text, _) =>
          val safeText = JsonEncoder.string.encodeJson(text)
          out.write(s"""{"type": "success", "text": $safeText }""")
        case LLMResponse.SensitiveDataFound(_) =>
          out.write(
            s"""{"type": "sensitive-info", "text": "Sensitive info found. Please approve the request."}"""
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
        _ <- stateRepository.saveState(
          clientId.toString,
          InProgress(Dialogue.empty(clientId.toString))
        )
        _ <- channel.receiveAll {
          case Read(WebSocketFrame.Text(text)) =>
            (for {
              stateMayBe <- stateRepository.getState(clientId.toString)
              state <- ZIO
                .fromOption(stateMayBe)
                .mapError(_ => StateNotFound)
              _ <- state match {
                case RequestState.WaitingForApprove(fullDialogue) =>
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
                            fullDialogue.copy(secure = false)
                          )
                          .tap(llmResponse =>
                            stateRepository.saveState(
                              clientId.toString,
                              InProgress(llmResponse.dialogue.copy(secure = true))
                            )
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
                    _ <- ZIO.logInfo(
                      s"[$clientId] Got user prompt - $userPrompt"
                    )
                    state <- stateRepository.getState(clientId.toString)
                    _ <- ZIO.logInfo(s"[$clientId] Load State - $state")
                    dialogueHistory = state
                      .map(_.dialogue)
                      .fold(
                        Dialogue.startSecure(clientId.toString, userPrompt)
                      )(fullDialogue =>
                        fullDialogue
                          .copy(data =
                            fullDialogue.data ++ Dialogue
                              .startSecure(clientId.toString, userPrompt)
                              .data
                          )
                      )
                    modelResponse <- userRequestService.ask(
                      dialogueHistory
                    )
                    _ <- ZIO.logInfo(s"Model Response: $modelResponse")
                    _ <- modelResponse match {
                      case domain.LLMResponse.Success(
                            _,
                            fullDialogue
                          ) =>
                        stateRepository.saveState(
                          clientId.toString,
                          InProgress(fullDialogue)
                        )
                      case domain.LLMResponse.SensitiveDataFound(
                            fullDialogue
                          ) =>
                        stateRepository.saveState(
                          clientId.toString,
                          WaitingForApprove(fullDialogue)
                        )
                    }
                    _ <- channel.send(
                      Read(WebSocketFrame.Text(modelResponse.toJson))
                    )
                  } yield ()
              }
            } yield ()).catchAll(err =>
              ZIO.logError(err.getMessage) *> ZIO.logError(err.getStackTrace.mkString("\n")) *> channel.send(
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
