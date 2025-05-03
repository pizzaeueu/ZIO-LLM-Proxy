package com.github.pizzaeueu.domain

enum LLMModel(val tokenWindow: Int):
  case `ChatGPT-3.5-turbo` extends LLMModel(4096)