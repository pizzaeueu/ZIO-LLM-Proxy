package com.github.pizzaeueu.llm

import com.github.pizzaeueu.config.OpenAIConfig
import com.openai.client.okhttp.OpenAIOkHttpClient
import com.openai.models.chat.completions.*
import zio.{Task, ZIO, ZLayer}

import scala.jdk.CollectionConverters.*

trait LLMClient:
  def sendRequest(
      dialog: List[ChatCompletionMessageParam],
      tools: List[ChatCompletionTool]
  ): Task[ChatCompletion]

final case class LLMClientLive(config: OpenAIConfig) extends LLMClient:

  private val client = OpenAIOkHttpClient
    .builder()
    .apiKey(
      config.key
    )
    .build()

  override def sendRequest(
      dialog: List[ChatCompletionMessageParam],
      tools: List[ChatCompletionTool]
  ): Task[ChatCompletion] = ZIO.attempt {
    val chat = ChatCompletionCreateParams
      .builder()
      .model(config.model)
      .messages(dialog.asJava)
      .tools(tools.asJava)
      .build()
    client.chat().completions().create(chat)
  }

object LLMClientLive:
  def live = ZLayer.fromFunction(LLMClientLive.apply)
