package com.github.pizzaeueu.mcp

import com.github.pizzaeueu.config.MCPConfig
import io.modelcontextprotocol.client.transport.{
  ServerParameters,
  StdioClientTransport
}
import io.modelcontextprotocol.client.{McpClient, McpSyncClient}
import io.modelcontextprotocol.spec.McpSchema
import io.modelcontextprotocol.spec.McpSchema.{CallToolRequest, ListToolsResult}
import zio.*

import scala.jdk.CollectionConverters.*

trait ProxyMCPClient:
  def listTools: Task[List[ListToolsResult]]

  def getTextResult(callTools: List[CallToolRequest]): Task[List[String]]

final case class ProxyMCPClientLive(
    clients: List[McpSyncClient],
    config: List[MCPConfig]
) extends ProxyMCPClient:
  override def listTools: Task[List[ListToolsResult]] =
    ZIO.collectAllPar {
      clients.map(c => ZIO.succeed(c.listTools()))
    }

  def getTextResult(callTools: List[CallToolRequest]): Task[List[String]] =
    for {
      callTools <- ZIO.succeed {
        callTools
          .map { callTool => clients.head.callTool(callTool) }
      } // todo: run on proper client
      toolResult = callTools.flatMap(_.content().asScala)
      _ = println(s"toolResult = $toolResult")
      textResult <- ZIO.foreach(toolResult) {
        case content: McpSchema.TextContent =>
          ZIO.succeed(content.text())
        case content =>
          ZIO
            .logError(s"Unsupported MCP content type - ${content.`type`()}")
            .as("")
      }
    } yield textResult

object ProxyMCPClientLive {
  def live: ZLayer[List[MCPConfig], Nothing, ProxyMCPClient] = ZLayer.scoped {
    for {
      config <- ZIO.service[List[MCPConfig]]
      supportedMCPs = config.map { mcp =>
        ZIO.succeed(
          ServerParameters
            .builder(mcp.command)
            .args(mcp.args*)
            .build()
        ) <* ZIO.logInfo(s"new MCP server registered: ${mcp.name}")
      }
      supportedMCPsZIO <- ZIO.collectAllPar(supportedMCPs)
      transport = supportedMCPsZIO.map(StdioClientTransport(_))
      clients = transport
        .map(
          McpClient
            .sync(_)
            .build()
        )
      _ <- ZIO.succeed(clients.map(_.initialize()))
    } yield ProxyMCPClientLive(clients, config)
  }
}
