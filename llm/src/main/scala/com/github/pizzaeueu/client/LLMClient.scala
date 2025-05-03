package com.github.pizzaeueu.client

import com.github.pizzaeueu.config.OpenAIConfig
import com.github.pizzaeueu.domain.{Dialogue, LLMTool}
import com.github.pizzaeueu.service.TokenValidator
import com.openai.client.okhttp.OpenAIOkHttpClient
import com.openai.models.chat.completions.*
import zio.{Task, ZIO, ZLayer}

import scala.jdk.CollectionConverters.*

trait LLMClient:
  def sendRequest(
      dialogue: Dialogue,
      tools: List[LLMTool]
  ): Task[ChatCompletion]

final case class LLMClientLive(config: OpenAIConfig) extends LLMClient:

  private val client = OpenAIOkHttpClient
    .builder()
    .apiKey(
      config.key
    )
    .build()

  override def sendRequest(
      dialogue: Dialogue,
      tools: List[LLMTool]
  ): Task[ChatCompletion] = ZIO.fromEither(TokenValidator.validate(dialogue, config.model)) *>
    ZIO.attempt {
    val chat = ChatCompletionCreateParams
      .builder()
      .model(config.model.toString)
      .messages(dialogue.data.asJava)
      .tools(tools.map(_.tool).asJava)
      .build()
    client.chat().completions().create(chat)
  }

object LLMClientLive:
  def live = ZLayer.fromFunction(LLMClientLive.apply)
