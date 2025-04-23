package com.github.pizzaeueu.llm

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.github.pizzaeueu.config.OpenAIConfig
import com.github.pizzaeueu.mcp.ProxyMCPClient
import com.openai.client.okhttp.OpenAIOkHttpClient
import com.openai.core.JsonValue
import com.openai.models.chat.completions.{
  ChatCompletionCreateParams,
  ChatCompletionMessageParam,
  ChatCompletionTool,
  ChatCompletionUserMessageParam
}
import com.openai.models.{FunctionDefinition, FunctionParameters}
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult
import zio.{Task, ZIO, ZLayer}

import scala.annotation.nowarn
import scala.jdk.CollectionConverters.*

trait LLMClient:
  def sendRequest(prompt: String): Task[Unit]

final case class LLMClientLive(mcpClient: ProxyMCPClient, config: OpenAIConfig)
    extends LLMClient:
  private val mapper = new ObjectMapper()
  private val client = OpenAIOkHttpClient
    .builder()
    .apiKey(
      config.key
    )
    .build()

  @nowarn
  override def sendRequest(prompt: String): Task[Unit] = for {
    _ <- ZIO.logInfo(s"Prompt: $prompt")
    tools <- mcpClient.listTools
    openAiTools <- mapMcpToOpenAITools(tools)
    userRequest = List(
      ChatCompletionMessageParam.ofUser(
        ChatCompletionUserMessageParam
          .builder()
          .content(prompt)
          .build()
      )
    )
    params = {
      ChatCompletionCreateParams
        .builder()
        .model(config.model)
        .messages(userRequest.asJava)
        .tools(openAiTools.asJava)
        .build()
    }
    res = client.chat().completions().create(params)
    _ <- ZIO.logInfo(s"Model Response - ${res.choices().get(0).message()}")
    _ <- ZIO.logInfo(
      s"Model choice - ${res.choices().get(0).finishReason().known().toString}"
    )
  } yield ()

  private def mapMcpToOpenAITools(
      mcpTools: List[ListToolsResult]
  ): Task[List[ChatCompletionTool]] = ZIO.succeed {
    mcpTools.flatMap { tool =>
      tool
        .tools()
        .asScala
        .map { tool =>
          val jsonSchemaNode: JsonNode = mapper.valueToTree(tool.inputSchema())
          val jsonSchemaMap = jsonSchemaNode
            .fields()
            .asScala
            .map(entry =>
              entry.getKey -> JsonValue.fromJsonNode(entry.getValue)
            )
            .toMap
            .asJava

          val params = FunctionParameters
            .builder()
            .additionalProperties(jsonSchemaMap)
            .build()

          val function = FunctionDefinition
            .builder()
            .name(tool.name())
            .description(tool.description())
            .parameters(params)
            .build()

          ChatCompletionTool.builder().function(function).build()
        }
        .toList
    }
  }

object LLMClientLive:
  def live = ZLayer.fromFunction(LLMClientLive.apply)
