package com.github.pizzaeueu.client

import com.github.pizzaeueu.config.MCPConfig
import com.github.pizzaeueu.domain.*
import com.github.pizzaeueu.service.PIIChecker
import io.modelcontextprotocol.client.McpClient
import io.modelcontextprotocol.client.transport.{
  ServerParameters,
  StdioClientTransport
}
import io.modelcontextprotocol.spec.McpSchema
import zio.*

import scala.jdk.CollectionConverters.*

trait ProxyMCPClient:
  def listTools: Task[List[McpTools]]

  def getTextResult(
      mcpRequests: List[McpRequest]
  ): Task[List[McpResponseString]]

final case class ProxyMCPClientLive(
    piiChecker: PIIChecker,
    clients: List[McpClientExtended],
    config: List[MCPConfig]
) extends ProxyMCPClient:
  override def listTools: Task[List[McpTools]] =
    ZIO.succeed {
      clients.map(c => McpTools(c.clientId, c.mcpClient.listTools()))
    }

  def getTextResult(
      mcpRequests: List[McpRequest]
  ): Task[List[McpResponseString]] = {
    ZIO.collectAll(
      mcpRequests.map(request => runMcpRequest(request))
    )
  }

  private def findClientIdByTool(toolName: String): Task[String] = for {
    tools <- listTools
    filteredTools = tools.filter(tool =>
      tool.tool.tools().asScala.exists(_.name() == toolName)
    )
    res <-
      if (filteredTools.isEmpty) {
        ZIO.fail(McpClientNotFoundByTool(toolName))
      } else if (filteredTools.size > 1) {
        ZIO.fail(
          SeveralMcpClientsFound(toolName)
        )
      } else {
        ZIO.succeed(filteredTools.head.clientId)
      }
  } yield res

  private def runMcpRequest(mcpRequest: McpRequest): Task[McpResponseString] =
    for {
      clientId <- findClientIdByTool(mcpRequest.request.name())
      tool <- ZIO
        .fromOption(clients.find(_.clientId == clientId))
        .mapError(_ => McpClientNotFoundById(clientId))
      _ <- ZIO.logInfo(
        s"Running mcp request: ${mcpRequest.request.name()} for mcp client $clientId"
      )
      mcpResponse <- ZIO.attempt(tool.mcpClient.callTool(mcpRequest.request))
      _ <- ZIO.logInfo(
        s"mcp response: ${mcpResponse.content()} for mcp client $clientId"
      )
      textResultList <- ZIO.foreach(mcpResponse.content().asScala) {
        case content: McpSchema.TextContent if !mcpResponse.isError =>
          ZIO.succeed(content.text())
        case content if mcpResponse.isError =>
          ZIO
            .logError(
              s"Error during fetching mcp data - $content"
            )
            .as("")
        case content =>
          ZIO
            .logError(
              s"Unsupported MCP content type - id: $clientId, content: ${content.`type`()}"
            )
            .as("")
      }
      textResult = textResultList.mkString("\n")
      isSensitive <- piiChecker.containsSensitiveData(textResult)
    } yield McpResponseString(clientId, textResult, isSensitive)

object ProxyMCPClientLive {
  def live: ZLayer[List[MCPConfig] & PIIChecker, Nothing, ProxyMCPClient] =
    ZLayer.scoped {
      for {
        config <- ZIO.service[List[MCPConfig]]
        supportedMCPs = config.map { mcp =>
          ZIO.succeed(
            mcp.name -> ServerParameters
              .builder(mcp.command)
              .args(mcp.args*)
              .env(mcp.env.asJava)
              .build()
          ) <* ZIO.logInfo(s"new MCP server registered: ${mcp.name}")
        }
        supportedMCPsZIO <- ZIO.collectAllPar(supportedMCPs)
        transport = supportedMCPsZIO.map { case (name, params) =>
          name -> StdioClientTransport(params)
        }
        clients = transport
          .map { case (name, transport) =>
            McpClientExtended(
              name,
              McpClient
                .sync(transport)
                .build()
            )
          }
        _ <- ZIO.succeed(clients.map(_.mcpClient.initialize()))
        piiChecker <- ZIO.service[PIIChecker]
      } yield ProxyMCPClientLive(piiChecker, clients, config)
    }
}
