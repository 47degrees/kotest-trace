@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.fortyseven.kotest.trace.ktor

import com.fortyseven.kotest.trace.ArbModel
import com.fortyseven.kotest.trace.checkAgainst
import com.fortyseven.kotest.trace.formula.Formula
import com.fortyseven.kotest.trace.formula.Info
import com.fortyseven.kotest.trace.formula.Step
import io.ktor.client.HttpClient
import io.ktor.client.plugins.resources.resources
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.client.request.request as requestBuilder
import io.ktor.http.HttpMethod
import io.ktor.resources.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer


public typealias HttpFormula<R> = Formula<Info<RequestInfo<R>, HttpClient, HttpResponse>>

public data class RequestInfo<out R>(
  val method: HttpMethod,
  val resource: R,
  val serializer: KSerializer<@UnsafeVariance R>,
  val additional: HttpRequestBuilder.() -> Unit = { }
)

public object Trace {
  public inline fun <reified R> get(resource: R, noinline additional: HttpRequestBuilder.() -> Unit = { }): RequestInfo<R> =
    RequestInfo(HttpMethod.Get, resource, serializer(), additional)
  public inline fun <reified R> post(resource: R, noinline additional: HttpRequestBuilder.() -> Unit = { }): RequestInfo<R> =
    RequestInfo(HttpMethod.Get, resource, serializer(), additional)
  public inline fun <reified R> put(resource: R, noinline additional: HttpRequestBuilder.() -> Unit = { }): RequestInfo<R> =
    RequestInfo(HttpMethod.Get, resource, serializer(), additional)
  public inline fun <reified R> patch(resource: R, noinline additional: HttpRequestBuilder.() -> Unit = { }): RequestInfo<R> =
    RequestInfo(HttpMethod.Get, resource, serializer(), additional)
  public inline fun <reified R> delete(resource: R, noinline additional: HttpRequestBuilder.() -> Unit = { }): RequestInfo<R> =
    RequestInfo(HttpMethod.Get, resource, serializer(), additional)
  public inline fun <reified R> head(resource: R, noinline additional: HttpRequestBuilder.() -> Unit = { }): RequestInfo<R> =
    RequestInfo(HttpMethod.Get, resource, serializer(), additional)
  public inline fun <reified R> options(resource: R, noinline additional: HttpRequestBuilder.() -> Unit = { }): RequestInfo<R> =
    RequestInfo(HttpMethod.Get, resource, serializer(), additional)
}

public suspend inline fun <State, reified R: Any> HttpClient.checkAgainst(
  model: ArbModel<State, RequestInfo<R>>,
  range: IntRange = 1 .. 100,
  noinline formula: () -> HttpFormula<R>
): Unit = checkAgainst(
  model, this, { req, client -> Step(client, client.performRequest(req)) }, range, formula
)

@PublishedApi internal suspend inline fun <reified R: Any> HttpClient.performRequest(
  req: RequestInfo<R>
): HttpResponse {
  val resources = this.resources()
  return requestBuilder {
    method = req.method
    href(resources.resourcesFormat, req.serializer, req.resource, url)
    req.additional(this)
  }
}
