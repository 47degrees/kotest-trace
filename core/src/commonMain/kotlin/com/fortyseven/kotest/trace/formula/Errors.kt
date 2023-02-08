package com.fortyseven.kotest.trace.formula

public data class TraceAssertionError(
  public val trace: List<*>,
  public val state: Any?,
  public val problems: List<AssertionError>
): AssertionError()

public data class FalseError(
  override val message: String
): AssertionError(message)

public data class NegationWasTrue(
  public val formula: Formula<*>
): AssertionError("negated condition was true")

public data class ShouldHoldEventually(
  public val formula: Formula<*>
): AssertionError("should hold eventually")
