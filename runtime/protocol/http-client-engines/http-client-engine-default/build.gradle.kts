/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

description = "Default HTTP Client Engine for Smithy services generated by smithy-kotlin"
extra["moduleName"] = "aws.smithy.kotlin.runtime.http.engine"

val ktorVersion: String by project

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":runtime:protocol:http"))
                implementation(project(":runtime:logging"))
            }
        }
        jvmMain {
            dependencies {
                // okhttp works on both JVM and Android
                implementation(project(":runtime:protocol:http-client-engines:http-client-engine-okhttp"))
            }
        }
        all {
            languageSettings.optIn("aws.smithy.kotlin.runtime.util.InternalApi")
        }
    }
}
