package com.github.pizzaeueu.domain

enum RequestState(val dialogue: Dialogue) {
  case InProgress(d: Dialogue) extends RequestState(d)
  case WaitingForApprove(d: Dialogue) extends RequestState(d)
}
