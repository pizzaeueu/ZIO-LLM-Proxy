package com.github.pizzaeueu.domain

enum LLMModel(val tokenWindow: Int):
  case `gpt-3.5-turbo` extends LLMModel(4096)