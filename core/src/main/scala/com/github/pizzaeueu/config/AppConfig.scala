package com.github.pizzaeueu.config

import zio.Config
import zio.config.magnolia.*

final case class AppConfig(
    server: HttpServerConfig,
    openai: OpenAIConfig,
    mcp: List[MCPConfig]
) derives Config
