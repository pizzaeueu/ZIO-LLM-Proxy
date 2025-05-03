package com.github.pizzaeueu.config

import zio.Config
import zio.config.magnolia.*

case class OpenAIConfig(model: String, key: String) derives Config
