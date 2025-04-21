import zio.*

object Main extends ZIOAppDefault {

  override def run: ZIO[ZIOAppArgs & Scope, Any, Any] =
    ZIO.log("Hello World!")
}
