@file:Suppress("DSL_SCOPE_VIOLATION")

plugins {
  alias(libs.plugins.kotlin.multiplatform) apply false
  alias(libs.plugins.kotlinx.serialization) apply false
}

allprojects {
  repositories {
    mavenCentral()
  }

  group = "com.47deg.kotest"
  version = "0.1-SNAPSHOT"

  tasks.withType<Test>().configureEach {
    useJUnitPlatform()
  }
}
