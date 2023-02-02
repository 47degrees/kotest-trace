@file:Suppress("DSL_SCOPE_VIOLATION")

plugins {
  alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
  explicitApi()

  // set targets
  jvm {
    jvmToolchain(8)
  }
  js(IR) {
    browser()
    nodejs()
  }

  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(libs.kotest.frameworkEngine)
        implementation(libs.kotest.assertionsCore)
        implementation(libs.kotest.property)
        implementation(libs.kotlinx.coroutines)
      }
    }
    val commonTest by getting {
      dependencies {
        implementation(libs.kotest.frameworkEngine)
        implementation(libs.kotest.assertionsCore)
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
