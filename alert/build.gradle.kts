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
    val jvmMain by getting {
      dependencies {
        implementation(projects.kotestTraceCore)
        implementation(projects.kotestTraceKtor)
        implementation(libs.ktor.client.resources)
        implementation(libs.ktor.client.contentNegotiation)
        implementation(libs.ktor.server.resources)
        implementation(libs.ktor.server.contentNegotiation)
        implementation(libs.ktor.server.test)
        implementation(libs.ktor.client.resources)
        implementation(libs.kotlinx.serialization.json)
        implementation(libs.ktor.serialization.json)
        implementation(libs.kotest.frameworkEngine)
        implementation(libs.kotest.assertionsCore)
        implementation(libs.kotest.property)
        implementation(libs.kafka)
        implementation(libs.testcontainers.kafka)
        implementation(libs.arrow.fx.stm)
      }
    }
  }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().all {
  kotlinOptions.freeCompilerArgs += "-Xcontext-receivers"
}
