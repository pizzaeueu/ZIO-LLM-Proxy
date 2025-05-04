package com.github.pizzaeueu.config

import zio.config.magnolia.*
import zio.config.typesafe.TypesafeConfigProvider
import zio.{Config, ZIO}

final case class AppConfig(
    server: HttpServerConfig,
    openai: OpenAIConfig,
    pii: PiiConfig,
    mcpFilePath: String
)

object AppConfig {
  private val appDescriptor = deriveConfig[AppConfig]
  def loadConfigs(): ZIO[Any, Config.Error, (AppConfig, McpData)] = for {
    appConfig <- ZIO
      .config(appDescriptor)
      .withConfigProvider(TypesafeConfigProvider.fromResourcePath())
    mcpConfig <- TypesafeConfigProvider
      .fromHoconFilePath(appConfig.mcpFilePath)
      .load[McpData]
  } yield (appConfig, mcpConfig)
}
