package com.github.pizzaeueu.domain.llm

import com.openai.models.chat.completions.ChatCompletionTool

case class LLMTool(toolId: String, tool: ChatCompletionTool)
