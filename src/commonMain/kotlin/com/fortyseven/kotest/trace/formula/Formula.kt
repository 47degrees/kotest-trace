package com.fortyseven.kotest.trace.formula

import kotlin.reflect.KClass

/**
 * Formulae understood by the library.
 */
public sealed interface Formula<in A> {
  public operator fun not(): Formula<A> = when (this) {
    is TRUE -> FALSE
    is FALSE -> TRUE
    is Predicate -> Not(this)
    is Throws -> Not(this)
    is Not -> this.formula
    is And -> or(formulae.map { !it })
    is Or -> and(formulae.map { !it })
    is Implies -> {
      // since not (A => B) = not (not(A) or B) = A and not(B)
      and(`if`, !then)
    }
    is Next -> Next(!formula)
    is DependentNext -> DependentNext { x -> !formula(x) }
    is Always -> Eventually(!formula)
    is Eventually -> Always(!formula)
  }

  public fun pretty(): String = when (this) {
    is TRUE -> "true"
    is FALSE -> "false"
    is Predicate -> "{ $message }"
    is Throws -> "{ $message }"
    is Not -> "!${formula.pretty()}"
    is And -> formulae.joinToString(separator = " & ") { it.pretty() }
    is Or -> formulae.joinToString(separator = " | ") { it.pretty() }
    is Implies -> "${`if`.pretty()} => ${then.pretty()}"
    is Next -> "next { ${formula.pretty()} }"
    is DependentNext -> "next { * }"
    is Always -> "always { ${formula.pretty()} }"
    is Eventually -> "eventually { ${formula.pretty()} }"
  }
}

public fun <A, B> Atomic<A>.contramap(f: (B) -> A): Atomic<B> = when(this) {
  is Constant -> this
  is Predicate -> Predicate(message) { test(f(it)) }
  is Throws -> this
}

public fun <A, B> Formula<A>.contramap(f: (B) -> A): Formula<B> = when(this) {
  is Atomic<A> -> this.contramap(f)
  is Not -> Not(formula.contramap(f))
  is And -> And(formulae.map { it.contramap(f) })
  is Or -> Or(formulae.map { it.contramap(f) })
  is Implies -> Implies(`if`.contramap(f), then.contramap(f))
  is Next -> Next(formula.contramap(f))
  is DependentNext -> DependentNext { x -> formula(f(x)).contramap(f) }
  is Always -> Always(formula.contramap(f))
  is Eventually -> Eventually(formula.contramap(f))
}

/**
 * Atomic formulae contain *no* temporal operators.
 */
public sealed interface Atomic<in A>: Formula<A>

/**
 * Constant formulae.
 */
public sealed interface Constant: Atomic<Any?>
public object TRUE: Constant
public object FALSE: Constant

internal data class Predicate<in A>(val message: String, val test: (A) -> Boolean): Atomic<A>
internal data class Throws(val message: String, val test: (Throwable) -> Boolean): Atomic<Any?>

internal data class Not<in A>(val formula: Atomic<A>): Formula<A>

// logical operators
internal data class And<in A>(val formulae: List<Formula<A>>): Formula<A>
internal data class Or<in A>(val formulae: List<Formula<A>>): Formula<A>
// it's important that the left-hand side of => is atomic,
// because otherwise we cannot know at each step whether
// to go with the right-hand side or not
internal data class Implies<in A>(val `if`: Atomic<A>, val then: Formula<A>): Formula<A>

// temporal operators
internal data class Always<in A>(val formula: Formula<A>): Formula<A>
internal data class Eventually<in A>(val formula: Formula<A>): Formula<A>
internal data class Next<in A>(val formula: Formula<A>): Formula<A>
// this is key in this development (and different from others)
// the 'next' formula can depend on the *current* state
// we can do that because 'next' is always kept until the next round
internal data class DependentNext<in A>(val formula: (A) -> Formula<A>): Formula<A>

// nicer builders

/**
 * Basic formula which checks that an item is produced, and satisfies the [predicate].
 */
public fun <A> holds(message: String, predicate: (A) -> Boolean): Atomic<A> =
  Predicate(message, predicate)

/**
 * Basic formula which checks that an exception of type [T] has been thrown.
 */
public fun <T: Throwable> throws(klass: KClass<T>, message: String, predicate: (T) -> Boolean = { true }): Atomic<Any?> =
  Throws(message) {
    @Suppress("UNCHECKED_CAST")
    if (klass.isInstance(it)) predicate(it as T) else false
  }

/**
 * Basic formula which checks that an exception of type [T] has been thrown.
 */
public inline fun <reified T: Throwable> throws(message: String, noinline predicate: (T) -> Boolean = { true }): Atomic<Any?> =
  throws(T::class, message, predicate)

/**
 * Negation of a formula. Note that failure messages are not saved accross negation boundaries.
 */
public fun <A> not(formula: Formula<A>): Formula<A> = !formula

/**
 * Conjunction, `&&` operator.
 */
public fun <A> and(vararg formulae: Formula<A>): Formula<A> = and(formulae.toList())

/**
 * Conjunction, `&&` operator.
 */
public fun <A> and(formulae: List<Formula<A>>): Formula<A> {
  val filtered = formulae.flatMap {
    when (it) {
      TRUE -> emptyList()
      is And -> it.formulae
      else -> listOf(it)
    }
  }
  return if (filtered.isEmpty()) TRUE else And(filtered)
}

/**
 * Disjunction, `||` operator.
 */
public fun <A> or(vararg formulae: Formula<A>): Formula<A> = or(formulae.toList())

/**
 * Disjunction, `||` operator.
 */
public fun <A> or(formulae: List<Formula<A>>): Formula<A> {
  val filtered = formulae.flatMap {
    when (it) {
      FALSE -> emptyList()
      is Or -> it.formulae
      else -> listOf(it)
    }
  }
  return if (filtered.isEmpty()) FALSE else Or(filtered)
}

/**
 * Implication. `implies(oneWay, orAnother)` is satisfied if either `oneWay` is `false`,
 * or `oneWay && orAnother` is `true`.
 */
public fun <A> implies(`if`: Atomic<A>, then: Formula<A>): Formula<A> = Implies(`if`, then)

public fun <A> implies(condition: (A) -> Boolean, then: Formula<A>): Formula<A> =
  implies(holds("condition", condition), then)

public fun <A> implies(condition: (A) -> Boolean, then: () -> Formula<A>): Formula<A> =
  implies(holds("condition", condition), then())

/**
 * Next, specifies that the formula should hold in the next state.
 */
public fun <A> next(block: () -> Formula<A>): Formula<A> = Next(block())

/**
 * Next, specifies that the formula should hold in the next state.
 * That formula may depend on the current item being processed.
 */
public fun <A> next(block: (A) -> Formula<A>): Formula<A> = DependentNext(block)

/**
 * Specifies that the formula must be true in every state
 * after the current one.
 */
public fun <A> afterwards(block: (A) -> Formula<A>): Formula<A> =
  next { current -> always { block(current) } }

/**
 * Always, specifies that the formula should hold for every item in the sequence.
 */
public fun <A> always(block: () -> Formula<A>): Formula<A> = Always(block())

/**
 * Always, specifies that the formula should hold for at least one item in the sequence,
 * before the sequence finishes.
 */
public fun <A> eventually(block: () -> Formula<A>): Formula<A> = Eventually(block())
