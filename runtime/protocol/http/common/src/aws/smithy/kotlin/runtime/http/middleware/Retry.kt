/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.smithy.kotlin.runtime.http.middleware

import aws.smithy.kotlin.runtime.http.operation.*
import aws.smithy.kotlin.runtime.http.operation.deepCopy
import aws.smithy.kotlin.runtime.http.request.HttpRequestBuilder
import aws.smithy.kotlin.runtime.io.Handler
import aws.smithy.kotlin.runtime.retries.RetryStrategy
import aws.smithy.kotlin.runtime.retries.getOrThrow
import aws.smithy.kotlin.runtime.retries.policy.RetryDirective
import aws.smithy.kotlin.runtime.retries.policy.RetryPolicy
import aws.smithy.kotlin.runtime.tracing.TraceSpan
import aws.smithy.kotlin.runtime.tracing.debug
import aws.smithy.kotlin.runtime.tracing.traceSpan
import aws.smithy.kotlin.runtime.tracing.withChildTraceSpan
import aws.smithy.kotlin.runtime.util.InternalApi
import kotlin.coroutines.coroutineContext

/**
 * Retry requests with the given strategy and policy
 * @param strategy the [RetryStrategy] to retry failed requests with
 * @param policy the [RetryPolicy] used to determine when to retry
 */
@InternalApi
public open class Retry<O>(
    protected val strategy: RetryStrategy,
    protected val policy: RetryPolicy<Any?>,
) : MutateMiddleware<O> {

    override suspend fun <H : Handler<SdkHttpRequest, O>> handle(request: SdkHttpRequest, next: H): O =
        if (request.subject.isRetryable) {
            var attempt = 1

            // FIXME this is the wrong span because we want the fresh one from inside each attempt but there's no way to
            // wire that through without changing the `RetryPolicy` interface
            val wrappedPolicy = PolicyLogger(policy, coroutineContext.traceSpan)

            val outcome = strategy.retry(wrappedPolicy) {
                coroutineContext.withChildTraceSpan("Attempt-$attempt") {
                    if (attempt > 1) {
                        coroutineContext.debug<Retry<*>> { "retrying request, attempt $attempt" }
                    }

                    // Deep copy the request because later middlewares (e.g., signing) mutate it
                    val requestCopy = request.deepCopy()

                    onAttempt(requestCopy, attempt)

                    attempt++
                    next.call(requestCopy)
                }
            }
            outcome.getOrThrow()
        } else {
            // Create a child span even though we won't retry
            coroutineContext.withChildTraceSpan("Non-retryable attempt") {
                next.call(request)
            }
        }

    /**
     * Hook for subclasses to intercept on attempt start
     * @param request the request for this attempt
     * @param attempt the current attempt number (1 based)
     */
    protected open fun onAttempt(request: SdkHttpRequest, attempt: Int) {}
}

/**
 * Wrapper around [policy] that logs termination decisions
 */
private class PolicyLogger(
    private val policy: RetryPolicy<Any?>,
    private val traceSpan: TraceSpan,
) : RetryPolicy<Any?> {
    override fun evaluate(result: Result<Any?>): RetryDirective = policy.evaluate(result).also {
        if (it is RetryDirective.TerminateAndFail) {
            traceSpan.debug<Retry<*>> { "request failed with non-retryable error" }
        }
    }
}

/**
 * Indicates whether this HTTP request could be retried. Some requests with streaming bodies are unsuitable for
 * retries.
 */
private val HttpRequestBuilder.isRetryable: Boolean
    get() = !body.isOneShot
