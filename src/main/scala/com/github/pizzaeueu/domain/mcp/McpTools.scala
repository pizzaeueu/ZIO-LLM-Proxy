package com.github.pizzaeueu.domain.mcp

import io.modelcontextprotocol.spec.McpSchema.ListToolsResult

case class McpTools(clientId: String, tool: ListToolsResult)
