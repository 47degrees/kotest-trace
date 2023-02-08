package com.fortyseven.kotest.trace

import com.fortyseven.kotest.trace.formula.*
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.property.Arb
import io.kotest.property.arbitrary.enum

enum class Action {
  INCREMENT, READ
}

val model: ArbModel<Unit, Action> = StatelessArbModel { Arb.enum<Action>() }

fun right(action: Action, state: Int): Step<Int, Int> =
  when (action) {
    Action.INCREMENT -> Step(state = state + 1, response = 0)
    Action.READ -> Step(state = state, response = state + 1)
  }

fun wrong(action: Action, state: Int): Step<Int, Int> =
  when (action) {
    Action.INCREMENT -> Step(state = state + 1, response = 0)
    Action.READ -> {
      Step(state = state, response = if (state == 2) 0 else state + 1)
    }
  }

class StateMachineSpec: StringSpec({
  "at least zero" {
    checkAgainst(model, -2, ::right) {
      always {
        impliesShould({ it.action == Action.READ }) {
          it.response.shouldBeGreaterThanOrEqual(0)
        }
      }
    }
  }
  "always increasing" {
    checkAgainst(model, 0, ::wrong) {
      always {
        implies({ it.action == Action.READ }) {
          afterwards { previous ->
            impliesShould({ it.action == Action.READ }) {
              it.response.shouldBeGreaterThanOrEqual(previous.response)
            }
          }
        }
      }
    }
  }
})
