package com.github.pizzaeueu.repository

import com.github.pizzaeueu.domain.RequestState
import zio.*

trait ClientStateRepository {
  def saveState(clientId: String, state: RequestState): Task[Unit]
  def getState(clientId: String): Task[Option[RequestState]]
}

final case class ClientStateRepositoryLive(ref: Ref[Map[String, RequestState]])
    extends ClientStateRepository {
  override def saveState(clientId: String, state: RequestState): Task[Unit] =
    ref.update(_.updated(clientId, state))
  override def getState(clientId: String): Task[Option[RequestState]] =
    ref.get.map(_.get(clientId))
}

object ClientStateRepositoryLive {
  def live = ZLayer.scoped {
    for {
      ref <- Ref.make(Map.empty[String, RequestState])
    } yield ClientStateRepositoryLive(ref)
  }
}