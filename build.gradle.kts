@file:Suppress("DSL_SCOPE_VIOLATION")

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
