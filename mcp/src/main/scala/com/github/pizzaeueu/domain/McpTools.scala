package com.github.pizzaeueu.domain

import io.modelcontextprotocol.spec.McpSchema.ListToolsResult

case class McpTools(clientId: String, tool: ListToolsResult)
