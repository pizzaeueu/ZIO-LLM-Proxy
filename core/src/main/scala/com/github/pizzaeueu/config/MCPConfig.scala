package com.github.pizzaeueu.config

import zio.Config
import zio.config.magnolia.*

final case class MCPConfig(name: String, command: String, args: List[String])
    derives Config
final case class McpData(data: List[MCPConfig])
    derives Config
