package com.fortyseven.kotest.trace.formula

import kotlin.runCatching

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

public tailrec suspend fun <Action, State, Response> Formula<Info<Action, State, Response>>.check(
  actions: List<Action>,
  current: State,
  step: suspend (Action, State) -> Step<State, Response>,
  previousActions: MutableList<Action> = mutableListOf()
): Unit = when {
  actions.isEmpty() ->
    this.leftToProve().throwIfFailed(previousActions, current)
  else -> {
    val action = actions.first()
    val oneStepFurther = runCatching { step(action, current) }.map { Info(action, current, it.state, it.response) }
    val progress = this.check(oneStepFurther)
    previousActions.add(action)
    progress.result.throwIfFailed(previousActions, oneStepFurther)
    when (val next = oneStepFurther.getOrNull()) {
      null -> progress.next.leftToProve().throwIfFailed(previousActions, progress.next)
      else -> progress.next.check(actions.drop(1), next.nextState, step, previousActions)
    }
  }
}

private fun <A, T> FormulaStepResult.throwIfFailed(actions: List<A>, state: T) {
  when (this) {
    null -> { } // ok
    else -> throw TraceAssertionError(actions, state, this)
  }
}
