package com.github.pizzaeueu.llm

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
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
import zio.{Task, ZIO}

import scala.annotation.nowarn
import scala.jdk.CollectionConverters.*

trait LLMClient:
  def query(prompt: String): String

final case class LLMClientLive(mcpClient: ProxyMCPClient) extends LLMClient:
  private val mapper = new ObjectMapper()
  val client = OpenAIOkHttpClient.builder().apiKey("hahaha").build()

  @nowarn
  override def query(prompt: String): String = {

    for {
      tools <- mcpClient.listTools
      openAiTools <- mcpToolsToOpenAITools(tools)
      userRequest = List(
        ChatCompletionMessageParam.ofUser(
          ChatCompletionUserMessageParam
            .builder()
            .content("What is the price of AAPL?")
            .build()
        )
      )
      params = {
        ChatCompletionCreateParams
          .builder()
          .model("gpt-4o")
          .messages(userRequest.asJava)
          .tools(openAiTools.asJava)
          .build()
      }
      _ = client.chat().completions().create(params)
    } yield ()

    ???
  }

  private def mcpToolsToOpenAITools(
      mcpTools: ListToolsResult
  ): Task[List[ChatCompletionTool]] = ZIO.succeed {
    mcpTools
      .tools()
      .asScala
      .map { tool =>
        val jsonSchemaNode: JsonNode = mapper.valueToTree(tool.inputSchema())
        val jsonSchemaMap = jsonSchemaNode
          .fields()
          .asScala
          .map(entry => entry.getKey -> JsonValue.fromJsonNode(entry.getValue))
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

//object LLMClientLive:
//  def live = ZLayer.succeed(LLMClientLive())
