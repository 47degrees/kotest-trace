package com.fortyseven.kotest.trace.alert

// these things may happen in our system
public sealed interface Action {
  public data class Subscribe(val event: String, val user: String): Action
  public data class Event(val event: String, val info: String): Action
  public object ReadMessageQueue: Action
}

// this is the type in the Kafka queue
public data class Message(val event: String, val info: String, val user: String)
