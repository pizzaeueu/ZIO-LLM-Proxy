package com.github.pizzaeueu.domain

import com.openai.models.chat.completions.{
  ChatCompletionMessageParam,
  ChatCompletionUserMessageParam
}

case class Dialogue(
    id: String,
    data: List[ChatCompletionMessageParam],
    secure: Boolean
)

object Dialogue {
  def startSecure(id: String, prompt: UserPrompt): Dialogue = Dialogue(
    id,
    List(
      ChatCompletionMessageParam.ofUser(
        ChatCompletionUserMessageParam
          .builder()
          .content(prompt.text)
          .build()
      )
    ),
    true
  )
}
