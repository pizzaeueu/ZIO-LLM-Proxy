package com.github.pizzaeueu.service

import com.github.pizzaeueu.domain.LLMModel
import com.github.pizzaeueu.domain.{Dialogue, DialogueIsTooLarge}
import com.openai.models.chat.completions.ChatCompletionMessageParam

import scala.jdk.OptionConverters.*

//TODO: Should use tokenizer for each model + estimate response
object TokenValidator {
  def validate(
      dialogue: Dialogue,
      model: LLMModel,
  ): Either[DialogueIsTooLarge, Unit] = {
    val tokens = textToToken(
      dialogue.data.flatMap(extractContent).reduce(_ ++ _)
    )

    Either.cond(
      model.tokenWindow > tokens,
      (),
      DialogueIsTooLarge(model.tokenWindow, tokens)
    )
  }

  private def extractContent(
      message: ChatCompletionMessageParam
  ): Option[String] =
    List(
      message.user().flatMap(_.content().text()).toScala,
      message.assistant().map(_.content().toString).toScala,
      message.system().flatMap(_.content().text()).toScala,
      message.developer().flatMap(_.content().text()).toScala,
      message.tool().flatMap(_.content().text()).toScala
    ).flatten.reduceOption(_ ++ _)

  private def textToToken(text: String): Int = text.length / 4
}
