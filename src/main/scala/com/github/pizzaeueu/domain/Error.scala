package com.github.pizzaeueu.domain

import scala.util.control.NoStackTrace

trait Error extends RuntimeException with NoStackTrace:
  override def getMessage: String

case object StateNotFound extends Error {
  override def getMessage: String = "State not found"
}

case class McpClientNotFound(tool: String) extends Error {
  override def getMessage: String = s"MCP Client for tool $tool not found"
}

case class SeveralMcpClientsFound(tool: String) extends Error {
  override def getMessage: String = s"Several MCP Clients for tool $tool found"
}

case class ParsingError(message: String) extends Error {
  override def getMessage: String = s"Parsing Error: $message"
}

case class UnsupportedModelResponse(response: String) extends Error {
  override def getMessage: String = s"Unsupported Response from Model: $response"
}
