package com.github.pizzaeueu.services

import com.github.pizzaeueu.config.OpenAIConfig
import com.github.pizzaeueu.domain.*
import com.github.pizzaeueu.domain.RequestState.WaitingForApprove
import com.github.pizzaeueu.domain.llm.LLMResponse.Success
import com.github.pizzaeueu.domain.llm.{Dialogue, LLMResponse, LLMTool}
import com.github.pizzaeueu.domain.mcp.McpResponseString
import com.github.pizzaeueu.llm.LLMClient
import com.github.pizzaeueu.mcp.ProxyMCPClient
import com.github.pizzaeueu.repository.ClientStateRepository
import com.openai.models.chat.completions.*
import zio.{Task, ZIO, ZLayer}

import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.RichOptional

trait UserRequestService {
  def ask(
      dialogue: Dialogue
  ): Task[LLMResponse]
}

final case class UserRequestServiceLive(
    llmClient: LLMClient,
    mcpClient: ProxyMCPClient,
    stateRepository: ClientStateRepository,
    config: OpenAIConfig
) extends UserRequestService:
  override def ask(
      dialogue: Dialogue
  ): Task[LLMResponse] = {
    for {
      tools <- mcpClient.listTools.flatMap(mcpToolToOpenAiTool)
      modelResponse <- llmClient.sendRequest(dialogue, tools)
      res <- askModelUntilItIsReady(modelResponse, dialogue, tools)
    } yield res
  }

  private def askModelUntilItIsReady(
      modelResponse: ChatCompletion,
      previousDialogue: Dialogue,
      tools: List[LLMTool]
  ): Task[LLMResponse] = {
    ZIO.ifZIO(
      ZIO.succeed(
        isToolCalled(modelResponse)
      )
    )(
      loadMcpData(modelResponse).flatMap {
        case mcpData
            if mcpData.exists(_.isSensitive) && previousDialogue.secure =>
          stateRepository.saveState(
            previousDialogue.id,
            WaitingForApprove(previousDialogue)
          ) *> ZIO.succeed(LLMResponse.SensitiveDataFound)
        case mcpData =>
          val toolsCalledByLLM = toolsCalledByLLMUnsafe(modelResponse)
          val fullDialog = previousDialogue.copy(data =
            previousDialogue.data ++ List(
              ChatCompletionMessageParam.ofAssistant(
                ChatCompletionAssistantMessageParam
                  .builder()
                  .toolCalls(toolsCalledByLLM.asJava)
                  .build()
              ),
              ChatCompletionMessageParam.ofTool(
                ChatCompletionToolMessageParam
                  .builder()
                  .content(mcpData.map(_.response).mkString("\n"))
                  .toolCallId(
                    toolsCalledByLLM.head.id()
                  ) // TODO: proper tools support
                  .build()
              )
            )
          )
          for {
            llmResponse <- llmClient.sendRequest(fullDialog, tools)
            res <- askModelUntilItIsReady(llmResponse, fullDialog, tools)
          } yield res
      },
      getModelResponseAsText(modelResponse).map(Success.apply)
    )
  }

  private def toolsCalledByLLMUnsafe(modelResponse: ChatCompletion) =
    modelResponse
      .choices()
      .get(0)
      .message()
      .toolCalls()
      .get
      .asScala
      .toList

  private def loadMcpData(
      modelResponse: ChatCompletion
  ): Task[List[McpResponseString]] = {
    openAIToolToMcpTool(
      toolsCalledByLLMUnsafe(modelResponse)
    ).flatMap(mcpClient.getTextResult)
  }

  private def getModelResponseAsText(modelResponse: ChatCompletion) =
    ZIO
      .fromOption(
        modelResponse.choices().get(0).message().content().toScala
      )
      .mapError(_ => new RuntimeException("Unsupported model response"))

  private def isToolCalled(modelResponse: ChatCompletion) = modelResponse
    .choices()
    .get(0)
    .finishReason()
    .known()
    .name() == "TOOL_CALLS"

object UserRequestServiceLive {
  def live = ZLayer.fromFunction(UserRequestServiceLive.apply)
}
