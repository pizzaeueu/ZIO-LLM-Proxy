package com.github.pizzaeueu.domain

enum RequestState {
  case InProgress
  case WaitingForApprove(dialogue: Dialogue)
}
