/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.smithy.kotlin.runtime.auth.awssigning

import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awssigning.tests.testCredentialsProvider
import aws.smithy.kotlin.runtime.http.Headers
import aws.smithy.kotlin.runtime.http.HttpBody
import aws.smithy.kotlin.runtime.http.HttpMethod
import aws.smithy.kotlin.runtime.http.parameters
import aws.smithy.kotlin.runtime.http.request.*
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.util.net.Host
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultRequestMutatorTest {
    @Test
    fun testAppendAuthHeader() {
        val canonical = CanonicalRequest(baseRequest.toBuilder(), "", "action;host;x-amz-date", "")
        val credentials = Credentials("", "secret key")
        val signature = "0123456789abcdef"

        val config = AwsSigningConfig {
            region = "us-west-2"
            service = "fooservice"
            signingDate = Instant.fromIso8601("20220427T012345Z")
            credentialsProvider = testCredentialsProvider
            omitSessionToken = true
        }

        val mutated = RequestMutator.Default.appendAuth(config, canonical, credentials, signature)

        assertEquals(baseRequest.method, mutated.method)
        assertEquals(baseRequest.url.toString(), mutated.url.toString())
        assertEquals(baseRequest.body, mutated.body)

        val expectedCredentialScope = "20220427/us-west-2/fooservice/aws4_request"
        val expectedAuthValue =
            "AWS4-HMAC-SHA256 Credential=${credentials.accessKeyId}/$expectedCredentialScope, " +
                "SignedHeaders=${canonical.signedHeaders}, Signature=$signature"
        val expectedHeaders = Headers {
            appendAll(baseRequest.headers)
            append("Authorization", expectedAuthValue)
        }.entries()

        assertEquals(expectedHeaders, mutated.headers.entries())
    }
}

private val baseRequest = HttpRequest {
    method = HttpMethod.GET
    url {
        host = Host.Domain("foo.com")
        path = "bar/baz"
        parameters {
            append("a", "apple")
            append("b", "banana")
            append("c", "cherry")
        }
    }
    headers {
        append("d", "durian")
        append("e", "elderberry")
        append("f", "fig")
    }
    body = HttpBody.fromBytes("hello world!".encodeToByteArray())
}
