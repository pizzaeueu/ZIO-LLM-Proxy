package com.github.pizzaeueu.config

import zio.Config
import zio.config.magnolia.*
import zio.json.*

final case class MCPConfig(name: String, command: String, args: List[String])
    derives Config,
      JsonDecoder,
      JsonEncoder
final case class McpData(data: List[MCPConfig])
    derives JsonDecoder,
      JsonEncoder

object McpData {
  given mcpDataConfig: Config[McpData] =
    Config.string.mapOrFail { jsonString =>
      val cleanJson = jsonString.trim
        .stripPrefix("\"")
        .stripSuffix("\"")
        .replaceAll("\\\\\"", "\"")
      cleanJson
        .fromJson[McpData]
        .left
        .map[Config.Error](error =>
          Config.Error
            .InvalidData(message = s"Failed to parse McpData JSON: $error")
        )
    }
}
