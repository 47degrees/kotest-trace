package com.fortyseven.kotest.trace.formula

// Based on the Quickstrom paper
// https://arxiv.org/pdf/2203.11532.pdf,
// which is in turned based on formula progression
// https://users.cecs.anu.edu.au/~thiebaux/papers/icaps05.pdf

internal typealias FormulaStepResult = List<AssertionError>?
private val everythingOk: FormulaStepResult = null
private fun FormulaStepResult.isOk() = this == null
private fun List<FormulaStepResult>.andResults() =
  if (all { it.isOk() })
    everythingOk
  else
    flatMap { it.orEmpty() }
private fun List<FormulaStepResult>.orResults() =
  if (any { it.isOk() })
    everythingOk
  else
    flatMap { it.orEmpty() }

public data class FormulaStep<A>(val result: FormulaStepResult, val next: Formula<A>)

public suspend fun <A> Atomic<A>.checkAtomic(x: Result<A>): FormulaStepResult = try {
  this.test(x)
  null
} catch(e: AssertionError) {
  listOf(e)
}

public suspend fun <A> Formula<A>.check(x: Result<A>): FormulaStep<A> = when (this) {
  is Atomic ->
    FormulaStep(this.checkAtomic(x), TRUE)
  is Not -> {
    if (formula.checkAtomic(x).isOk()) {
      FormulaStep(listOf(NegationWasTrue(formula)), TRUE)
    } else {
      FormulaStep(everythingOk, TRUE)
    }
  }
  is And -> {
    val steps = formulae.map { it.check(x) }
    val result = steps.map { it.result }.andResults()
    FormulaStep(result, and(steps.map { it.next }))
  }
  is Or -> {
    val steps = formulae.map { it.check(x) }
    val result = steps.map { it.result }.orResults()
    FormulaStep(result, or(steps.map { it.next }))
  }
  is Implies -> {
    val leftResult = `if`.checkAtomic(x)
    if (leftResult.isOk()) {
      // if left is true, we check the right
      then.check(x)
    } else {
      // otherwise the formula is true (false => x == true)
      FormulaStep(everythingOk, TRUE)
    }
  }
  is Next -> FormulaStep(everythingOk, formula)
  is DependentNext -> {
    x.fold(
      // we use the current value to compute the next step
      onSuccess = { FormulaStep(everythingOk, formula(it)) },
      // there are no more steps after an exception
      onFailure = { FormulaStep(everythingOk, TRUE) }
    )
  }
  is Always -> {
    // when we have always it has to be true
    // 1. in this state,
    // 2. in any other next state
    val step = formula.check(x)
    FormulaStep(step.result, and(step.next, this))
  }
  is Eventually -> {
    val step = formula.check(x)
    if (step.result.isOk()) {
      // this one is true, so we're done
      FormulaStep(everythingOk, TRUE)
    } else {
      // we have to try in the next one
      // so if we are done we haven't proved it yet
      FormulaStep(everythingOk,this)
    }
  }
}

// is there something missing to prove?
// if we have 'eventually', we cannot conclude
public fun <A> Formula<A>.leftToProve(): FormulaStepResult = when (this) {
  // atomic predicates are done
  is Atomic -> everythingOk
  is Not -> everythingOk
  // if we have 'and' and 'or', combine
  is And -> formulae.map { it.leftToProve() }.andResults()
  is Or -> formulae.map { it.leftToProve() }.orResults()
  is Implies -> listOf(`if`.leftToProve(), then.leftToProve()).andResults()
  // we have nothing missing here
  is Next -> everythingOk
  is DependentNext -> everythingOk
  is Always -> everythingOk
  // we have an 'eventually' missing
  is Eventually -> listOf(ShouldHoldEventually(this.formula))
}
