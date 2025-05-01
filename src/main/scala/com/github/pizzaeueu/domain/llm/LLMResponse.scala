package com.github.pizzaeueu.domain.llm

enum LLMResponse {
  case Success(text: String)
  case SensitiveDataFound
}