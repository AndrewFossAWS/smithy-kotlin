/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.smithy.kotlin.runtime.http.request

import aws.smithy.kotlin.runtime.http.*
import aws.smithy.kotlin.runtime.http.content.ByteArrayContent
import aws.smithy.kotlin.runtime.http.util.CanDeepCopy
import aws.smithy.kotlin.runtime.io.*
import aws.smithy.kotlin.runtime.util.InternalApi

/**
 * Used to construct an HTTP request
 * @param method The HTTP method (verb) to use when making the request
 * @param url Endpoint to make request to
 * @param headers HTTP headers
 * @param body Outgoing payload. Initially empty
 */
public class HttpRequestBuilder private constructor(
    public var method: HttpMethod,
    public val url: UrlBuilder,
    public val headers: HeadersBuilder,
    public var body: HttpBody,
) : CanDeepCopy<HttpRequestBuilder> {
    public constructor() : this(HttpMethod.GET, UrlBuilder(), HeadersBuilder(), HttpBody.Empty)

    public fun build(): HttpRequest =
        HttpRequest(method, url.build(), if (headers.isEmpty()) Headers.Empty else headers.build(), body)

    override fun deepCopy(): HttpRequestBuilder =
        HttpRequestBuilder(method, url.deepCopy(), headers.deepCopy(), body)

    override fun toString(): String = buildString {
        append("HttpRequestBuilder(method=$method, url=$url, headers=$headers, body=$body)")
    }
}

// convenience extensions

/**
 * Modify the URL inside the block
 */
public fun HttpRequestBuilder.url(block: UrlBuilder.() -> Unit) {
    url.apply(block)
}

/**
 * Set values from an existing [Url] instance
 */
public fun HttpRequestBuilder.url(value: Url) {
    url.apply {
        scheme = value.scheme
        host = value.host
        port = value.port
        path = value.path
        parameters.appendAll(value.parameters)
        fragment = value.fragment
        userInfo = value.userInfo
        forceQuery = value.forceQuery
    }
}

/**
 * Modify the headers inside the given block
 */
public fun HttpRequestBuilder.headers(block: HeadersBuilder.() -> Unit) {
    headers.apply(block)
}

/**
 * Add a single header. This will append to any existing headers with the same name.
 */
public fun HttpRequestBuilder.header(name: String, value: String): Unit = headers.append(name, value)

/**
 * Dump a debug description of the request
 *
 * @param dumpBody Flag controlling whether to also dump the body out. If true the body will be consumed and
 * replaced.
 */
@InternalApi
public suspend fun dumpRequest(request: HttpRequestBuilder, dumpBody: Boolean): String {
    val buffer = SdkBuffer()

    // TODO - we have no way to know the http version at this level to set HTTP/x.x
    buffer.writeUtf8("${request.method} ${request.url.encodedPath}\r\n")
    buffer.writeUtf8("Host: ${request.url.host}\r\n")

    val contentLength = request.headers["Content-Length"]?.toLongOrNull() ?: (request.body.contentLength ?: 0)
    if (contentLength > 0) {
        buffer.writeUtf8("Content-Length: $contentLength\r\n")
    }

    val skip = setOf("Host", "Content-Length")
    request.headers.entries()
        .filterNot { it.key in skip }
        .forEach {
            buffer.writeUtf8(it.value.joinToString(separator = ";", prefix = "${it.key}: ", postfix = "\r\n"))
        }

    buffer.writeUtf8("\r\n")

    if (dumpBody) {
        when (val body = request.body) {
            is HttpBody.Bytes -> buffer.write(body.bytes())
            is HttpBody.ChannelContent, is HttpBody.SourceContent -> {
                // consume the stream and replace the body
                val content = body.readAll()
                if (content != null) {
                    buffer.write(content)
                    request.body = ByteArrayContent(content)
                }
            }
            is HttpBody.Empty -> { } // nothing to dump
        }
    }

    return buffer.readUtf8()
}
