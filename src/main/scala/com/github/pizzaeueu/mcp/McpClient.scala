package com.github.pizzaeueu.mcp

import io.modelcontextprotocol.client.transport.{ServerParameters, StdioClientTransport}
import io.modelcontextprotocol.client.{McpClient, McpSyncClient}
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult
import zio.*

import java.nio.file.Paths

trait ProxyMCPClient:
  def listTools: Task[ListToolsResult]

final case class ProxyMCPClientLive(client: McpSyncClient) extends ProxyMCPClient:
  override def listTools: Task[ListToolsResult] = ZIO.succeed(client.listTools())

//TODO: Rewrite asap
object ProxyMCPClientLive {
  def live: ZLayer[Any, Nothing, ProxyMCPClient] = ZLayer.scoped {
    val testDataPath = Paths.get("data").toAbsolutePath
    for {
      params <- ZIO.succeed(
        ServerParameters
          .builder("docker")
          .args(
            "run",
            "-i",
            "--rm",
            "--mount",
            "type=bind,src=" + testDataPath + ",dst=/projects/resources",
            "mcp/filesystem",
            "/projects"
          )
          .build()
      )
      transport <- ZIO.succeed(StdioClientTransport(params))
      client <- ZIO.succeed(
        McpClient
          .sync(transport)
          .build()
      )
      _ <- ZIO.succeed(client.initialize())
    } yield ProxyMCPClientLive(client)
  }
}
