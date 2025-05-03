ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.6.4"

lazy val root = (project in file("."))
  .settings(
    name := "zio-llm-proxy"
  )
  .settings(
    libraryDependencies ++= ZIO.all ++ Logging.all ++ Testing.all :+ MCP.mcpSdk :+ OpenAI.sdk
  )
  .settings(
    assembly / assemblyMergeStrategy := (_ => MergeStrategy.first),
    Compile / mainClass := Some("com.github.pizzaeueu.Main")
  )
  .settings(
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
  .dependsOn(llm, core, pii, mcp)

lazy val core = project
  .settings(
    libraryDependencies ++= ZIO.all ++ Logging.all ++ Testing.all :+ MCP.mcpSdk :+ OpenAI.sdk
  )
  .settings(
    assembly / assemblyMergeStrategy := (_ => MergeStrategy.first)
  )

lazy val pii = project
  .settings(
    libraryDependencies ++= ZIO.all ++ Logging.all ++ Testing.all :+ MCP.mcpSdk :+ OpenAI.sdk
  )
  .settings(
    assembly / assemblyMergeStrategy := (_ => MergeStrategy.first)
  )
  .dependsOn(core)

lazy val llm = project
  .settings(
    libraryDependencies ++= ZIO.all ++ Logging.all ++ Testing.all :+ MCP.mcpSdk :+ OpenAI.sdk
  )
  .settings(
    assembly / assemblyMergeStrategy := (_ => MergeStrategy.first)
  )
  .dependsOn(core)

lazy val mcp = project
  .settings(
    libraryDependencies ++= ZIO.all ++ Logging.all ++ Testing.all :+ MCP.mcpSdk :+ OpenAI.sdk
  )
  .settings(
    assembly / assemblyMergeStrategy := (_ => MergeStrategy.first)
  )
  .dependsOn(core, pii)
