package com.github.pizzaeueu.domain

import com.github.pizzaeueu.domain.llm.Dialogue

enum RequestState {
  case InProgress
  case WaitingForApprove(dialogue: Dialogue)
}
