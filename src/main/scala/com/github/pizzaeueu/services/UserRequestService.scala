package com.github.pizzaeueu.services

import com.github.pizzaeueu.config.OpenAIConfig
import com.github.pizzaeueu.llm.LLMClient
import com.github.pizzaeueu.mcp.ProxyMCPClient
import com.openai.models.chat.completions.*
import zio.{Task, ZIO, ZLayer}

import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.RichOptional

trait UserRequestService {
  def ask(prompt: String): Task[String]
}

final case class UserRequestServiceLive(
    llmClient: LLMClient,
    mcpClient: ProxyMCPClient,
    config: OpenAIConfig
) extends UserRequestService:
  override def ask(prompt: String): Task[String] = {
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
  ): Task[String] = {
    ZIO.ifZIO(
      ZIO.succeed(
        isToolCalled(modelResponse)
      )
    )(
      sendToolResponseToModel(modelResponse, dialog, tools),
      getModelResponseAsText(modelResponse)
    )
  }

  private def sendToolResponseToModel(
      modelResponse: ChatCompletion,
      dialog: List[ChatCompletionMessageParam],
      tools: List[ChatCompletionTool]
  ) = {
    val tc = modelResponse
      .choices()
      .get(0)
      .message()
      .toolCalls()
      .get
      .asScala
      .toList
    for {
      toolsResponse <- openAIToolToMcpTool(
        tc
      ).flatMap(mcpClient.getTextResult)
      fullDialog = List(
        ChatCompletionMessageParam.ofAssistant(
          ChatCompletionAssistantMessageParam
            .builder()
            .toolCalls(tc.asJava)
            .build()
        ),
        ChatCompletionMessageParam.ofTool(
          ChatCompletionToolMessageParam
            .builder()
            .content(toolsResponse.mkString("\n"))
            .toolCallId(tc.head.id())
            .build()
        )
      ) ++ dialog
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
