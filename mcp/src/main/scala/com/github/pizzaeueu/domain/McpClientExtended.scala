package com.github.pizzaeueu.domain

import io.modelcontextprotocol.client.McpSyncClient

case class McpClientExtended(clientId: String, mcpClient: McpSyncClient)
