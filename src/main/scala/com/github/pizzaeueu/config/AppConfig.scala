package com.github.pizzaeueu.config

import zio.Config
import zio.config.magnolia.*

final case class AppConfig(
    server: HttpServerConfig
) derives Config
