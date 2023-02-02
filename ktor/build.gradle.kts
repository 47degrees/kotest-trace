@file:Suppress("DSL_SCOPE_VIOLATION")

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.kotlinx.serialization)
}

kotlin {
  explicitApi()

  // set targets
  jvm {
    jvmToolchain(8)
  }

  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(projects.kotestTraceCore)
        implementation(libs.kotest.frameworkEngine)
        implementation(libs.kotest.assertionsCore)
        implementation(libs.kotest.property)
        implementation(libs.ktor.client.resources)
      }
    }
    val commonTest by getting {
      dependencies {
        implementation(libs.kotest.frameworkEngine)
        implementation(libs.kotest.assertionsCore)
        implementation(libs.ktor.client.resources)
        implementation(libs.ktor.client.contentNegotiation)
        implementation(libs.ktor.server.resources)
        implementation(libs.ktor.server.contentNegotiation)
        implementation(libs.ktor.server.test)
        implementation(libs.kotlinx.serialization.json)
        implementation(libs.ktor.serialization.json)
      }
    }
    val jvmMain by getting
    val jvmTest by getting {
      dependencies {
        runtimeOnly(libs.kotest.runnerJUnit5)
      }
    }
  }
}
