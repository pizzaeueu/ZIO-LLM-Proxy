package com.github.pizzaeueu.services

import com.github.pizzaeueu.config.OpenAIConfig
import com.github.pizzaeueu.domain.*
import com.github.pizzaeueu.domain.LLMResponse.Success
import com.github.pizzaeueu.domain.llm.LLMTool
import com.github.pizzaeueu.domain.mcp.McpResponseString
import com.github.pizzaeueu.llm.LLMClient
import com.github.pizzaeueu.mcp.ProxyMCPClient
import com.github.pizzaeueu.pii.PIIChecker
import com.openai.models.chat.completions.*
import zio.{Task, ZIO, ZLayer}

import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.RichOptional

trait UserRequestService {
  def ask(prompt: String): Task[LLMResponse]
}

final case class UserRequestServiceLive(
    llmClient: LLMClient,
    mcpClient: ProxyMCPClient,
    piiChecker: PIIChecker,
    config: OpenAIConfig
) extends UserRequestService:
  override def ask(prompt: String): Task[LLMResponse] = {
    for {
      tools <- mcpClient.listTools.flatMap(mcpToolToOpenAiTool)
      userRequest = ChatCompletionMessageParam.ofUser(
        ChatCompletionUserMessageParam
          .builder()
          .content(prompt)
          .build()
      )
      modelResponse <- llmClient.sendRequest(List(userRequest), tools)
      res <- askModelUntilItIsReady(modelResponse, List(userRequest), tools)
    } yield res
  }

  private def askModelUntilItIsReady(
      modelResponse: ChatCompletion,
      dialog: List[ChatCompletionMessageParam],
      tools: List[LLMTool]
  ): Task[LLMResponse] = {
    ZIO.ifZIO(
      ZIO.succeed(
        isToolCalled(modelResponse)
      )
    )(
      loadMcpData(modelResponse).flatMap {
        case mcpData if mcpData.exists(_.isSensitive) =>
          ZIO.succeed(LLMResponse.SensitiveDataFound)
        case mcpData =>
          val toolsCalledByLLM = toolsCalledByLLMUnsafe(modelResponse)
          val fullDialog = List(
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
          ) ++ dialog
          for {
            llmResponse <- llmClient.sendRequest(fullDialog, tools)
            res <- askModelUntilItIsReady(llmResponse, dialog, tools)
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
