package com.github.pizzaeueu.domain

enum LLMResponse {
  case Success(text: String)
  case SensitiveDataFound
}