package com.github.pizzaeueu.config

import zio.Config
import zio.config.magnolia.*

final case class HttpServerConfig(host: String, port: Int) derives Config
