ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.6.4"

lazy val root = (project in file("."))
  .settings(
    name := "zio-llm-proxy"
  )
  .settings(
    libraryDependencies ++= ZIO.all ++ Logging.all ++ Testing.all :+ MCP.mcpSdk
  )
  .settings(
    assembly / assemblyMergeStrategy := (_ => MergeStrategy.first)
  )
  .settings(
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )

addCommandAlias("fmt", "scalafmtSbt; scalafmtAll;")
