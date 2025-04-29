package com.github.pizzaeueu.services

import com.github.pizzaeueu.config.OpenAIConfig
import com.github.pizzaeueu.llm.LLMClient
import com.github.pizzaeueu.mcp.ProxyMCPClient
import com.github.pizzaeueu.pii.PIIChecker
import com.openai.models.chat.completions.*
import zio.{Task, ZIO, ZLayer}
import com.github.pizzaeueu.domain.*
import com.github.pizzaeueu.domain.LLMResponse.Success

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
      tools: List[ChatCompletionTool]
  ): Task[LLMResponse] = {
    ZIO.ifZIO(
      ZIO.succeed(
        isToolCalled(modelResponse)
      )
    )(
      loadMcpData(modelResponse).flatMap {
        case (true, _) => ZIO.succeed(LLMResponse.SensitiveDataFound)
        case (false, mcpData) =>
          sendToolResponseToModel(
            mcpData,
            modelResponse
              .choices()
              .get(0)
              .message()
              .toolCalls()
              .get
              .asScala
              .toList,
            dialog,
            tools
          )
      },
      getModelResponseAsText(modelResponse).map(Success.apply)
    )
  }

  private def loadMcpData(
      modelResponse: ChatCompletion
  ): Task[(Boolean, List[String])] = {
    openAIToolToMcpTool(
      modelResponse
        .choices()
        .get(0)
        .message()
        .toolCalls()
        .get
        .asScala
        .toList
    ).flatMap(mcpClient.getTextResult).flatMap { mcpData =>
      piiChecker.containsSensitiveData(mcpData.mkString("\n")).map(_ -> mcpData)
    }
  }

  private def sendToolResponseToModel(
      toolsResponse: List[String],
      toolCalls: List[ChatCompletionMessageToolCall],
      dialog: List[ChatCompletionMessageParam],
      tools: List[ChatCompletionTool]
  ): Task[LLMResponse] = {
    val fullDialog = List(
      ChatCompletionMessageParam.ofAssistant(
        ChatCompletionAssistantMessageParam
          .builder()
          .toolCalls(toolCalls.asJava)
          .build()
      ),
      ChatCompletionMessageParam.ofTool(
        ChatCompletionToolMessageParam
          .builder()
          .content(toolsResponse.mkString("\n"))
          .toolCallId(toolCalls.head.id()) // TODO: proper tools support
          .build()
      )
    ) ++ dialog
    for {
      llmResponse <- llmClient.sendRequest(fullDialog, tools)
      res <- askModelUntilItIsReady(llmResponse, dialog, tools)
    } yield res
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
