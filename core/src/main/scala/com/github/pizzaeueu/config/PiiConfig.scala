package com.github.pizzaeueu.config

import zio.Config
import zio.config.magnolia.*

final case class PiiConfig(regex: String) derives Config
