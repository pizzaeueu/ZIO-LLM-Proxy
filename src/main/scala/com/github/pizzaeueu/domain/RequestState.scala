package com.github.pizzaeueu.domain

import com.github.pizzaeueu.domain.llm.Dialogue

enum RequestState {
  case Started
  case WaitingForApprove(dialogue: Dialogue)
  case Finished

}
