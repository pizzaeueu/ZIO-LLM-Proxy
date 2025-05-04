package com.github.pizzaeueu.domain

enum LLMResponse(val dialogue: Dialogue) {
  case Success(lastResponse: String, fullDialogue: Dialogue) extends LLMResponse(fullDialogue)
  case SensitiveDataFound(fullDialogue: Dialogue) extends LLMResponse(fullDialogue)
}