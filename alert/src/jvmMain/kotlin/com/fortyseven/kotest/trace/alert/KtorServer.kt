package com.fortyseven.kotest.trace.alert

import arrow.fx.stm.TMap
import arrow.fx.stm.atomically
import io.ktor.resources.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.resources.Resources
import io.ktor.server.resources.post
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

public object Routes {
  @Serializable @Resource("/subscribe")
  public class Subscribe() {
    @Serializable
    public data class Request(val event: String, val user: String)
  }

  @Serializable @Resource("/event")
  public class Event() {
    @Serializable
    public data class Request(val event: String, val info: String)
  }
}

// this is what we use to send the messages
// eventually a Kafka queue
public interface MessageSender {
  public suspend fun Message.send(): Unit
}

context(MessageSender) public suspend fun Application.server() {
  install(ContentNegotiation) { json() }
  install(Resources)

  val subscriptions: TMap<String, List<String>> = TMap.new()
  routing {
    post<Routes.Subscribe> {
      val req = call.receive<Routes.Subscribe.Request>()
      atomically {
        subscriptions[req.event] = subscriptions[req.event].orEmpty() + req.user
      }
      call.respond("OK")
    }

    post<Routes.Event> {
      val req = call.receive<Routes.Event.Request>()
      val users = atomically { subscriptions[req.event].orEmpty() }
      users.forEach { Message(req.event, req.info, it).send() }
    }
  }
}
