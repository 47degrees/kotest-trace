package com.fortyseven.kotest.trace.formula

import kotlin.runCatching
import io.kotest.assertions.fail

public data class Step<out State, out Response>(
  val state: State,
  val response: Response
)
public data class Info<out Action, out State, out Response>(
  val action: Action,
  val previousState: State,
  val nextState: State,
  val response: Response
)

public suspend fun <Action, State, Response> Formula<Info<Action, State, Response>>.check(
  actions: List<Action>,
  initial: State,
  step: suspend (Action, State) -> Step<State, Response>
) {
  var currentState = initial
  var currentFormula = this
  for (i in actions.indices) {
    val action = actions[i]
    runCatching { step(action, currentState) }.map {
      Info(action, initial, it.state, it.response)
    }.fold(
      onSuccess = {
        val progress = currentFormula.progress(Result.success(it))
        progress.result.check(actions.take(i + 1), it)
        currentState = it.nextState
        currentFormula = progress.next
      },
      onFailure = {
        val progress = currentFormula.progress(Result.failure(it))
        progress.result.check(actions.take(i + 1), it)
        progress.next.done().check(actions.take(i + 1), it)
        return@check // the check is finished if we get an exception
      }
    )
  }
  currentFormula.done().check(actions, currentState)
}

private fun <A, T> FormulaStepResult.check(actions: List<A>, state: T) {
  when (this) {
    null -> { } // ok
    else -> fail(
      "Formula falsified for $state:\n${joinToString(separator = "\n") { "- $it" }}\ntrace: $actions"
    )
  }
}
