/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    kotlin("plugin.serialization") version "1.7.10"
}

description = "Client runtime for Smithy services generated by smithy-kotlin"
extra["displayName"] = "Smithy :: Kotlin :: Client Runtime"
extra["moduleName"] = "aws.smithy.kotlin.runtime"

val coroutinesVersion: String by project

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                // io types are exposed as part of content/*
                // SdkClient also implements Closeable
                api(project(":runtime:io"))
                // Attributes property bag is exposed as client options
                api(project(":runtime:utils"))
                // Coroutines' locking features are used in retry token bucket implementations
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
            }
        }

        commonTest {
            dependencies {
                // Coroutines' locking features are used in retry token bucket implementations
                api("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")

                val kamlVersion: String by project
                implementation("com.charleskorn.kaml:kaml:$kamlVersion")

                implementation(project(":runtime:testing"))
            }
        }

        all {
            languageSettings.optIn("aws.smithy.kotlin.runtime.util.InternalApi")
        }
    }
}
