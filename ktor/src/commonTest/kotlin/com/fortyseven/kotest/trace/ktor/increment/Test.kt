package com.fortyseven.kotest.trace.ktor.increment

import com.fortyseven.kotest.trace.StatelessArbModel
import com.fortyseven.kotest.trace.formula.always
import com.fortyseven.kotest.trace.formula.holds
import com.fortyseven.kotest.trace.ktor.HttpFormula
import com.fortyseven.kotest.trace.ktor.RequestInfo
import com.fortyseven.kotest.trace.ktor.Trace
import com.fortyseven.kotest.trace.ktor.checkAgainst
import io.kotest.core.spec.style.StringSpec
import io.kotest.property.Arb
import io.kotest.property.arbitrary.choose
import io.kotest.property.arbitrary.constant
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.resources.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*

inline fun <reified R : Any> performTest(
  model: StatelessArbModel<RequestInfo<R>>,
  noinline formula: () -> HttpFormula<R>
): Unit = testApplication {
  application { counter() }
  val client = createClient {
    install(ContentNegotiation) { json() }
    install(Resources)
  }
  client.checkAgainst(model) { formula() }
}

val simpleModel: StatelessArbModel<RequestInfo<Any>> = StatelessArbModel {
  Arb.choose(
    1 to Arb.constant(Trace.get(Routes.Value())),
    1 to Arb.constant(Trace.post(Routes.Increment()))
  )
}

class StateMachineSpec: StringSpec({
  "at least zero" {
    performTest(simpleModel) {
      always {
        holds("non-negative") {
          when (it.action.resource) {
            is Routes.Value -> it.response.body<Int>() >= 0
            else -> true
          }
        }
      }
    }
  }
})
