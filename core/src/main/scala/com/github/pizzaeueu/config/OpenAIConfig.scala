package com.github.pizzaeueu.config

import zio.Config
import zio.config.magnolia.*
import com.github.pizzaeueu.domain.LLMModel

case class OpenAIConfig(model: LLMModel, key: String) derives Config
