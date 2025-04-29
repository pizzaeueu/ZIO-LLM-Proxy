package com.github.pizzaeueu

import com.fasterxml.jackson.databind.JsonNode
import com.openai.core.JsonValue
import com.openai.models.chat.completions.{
  ChatCompletionMessageToolCall,
  ChatCompletionTool
}
import com.openai.models.{FunctionDefinition, FunctionParameters}
import io.modelcontextprotocol.spec.McpSchema.{CallToolRequest, ListToolsResult}
import zio.{Task, ZIO}
import com.fasterxml.jackson.databind.ObjectMapper
import scala.jdk.CollectionConverters.*

package object services {
  private val mapper = new ObjectMapper()

  def mcpToolToOpenAiTool(
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

  def openAIToolToMcpTool(
      toolCalls: List[ChatCompletionMessageToolCall]
  ): Task[List[CallToolRequest]] = ZIO.attempt {
    toolCalls.map { toolCall =>
      val toolName = toolCall.function().name()
      val argumentsJson = toolCall.function().arguments()

      val argumentsMap: java.util.Map[String, Object] =
        mapper.readValue(
          argumentsJson,
          classOf[java.util.HashMap[String, Object]]
        )

      CallToolRequest(toolName, argumentsMap)
    }
  }
}
