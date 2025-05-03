package com.github.pizzaeueu.domain

import com.openai.models.chat.completions.ChatCompletionTool

case class LLMTool(toolId: String, tool: ChatCompletionTool)
