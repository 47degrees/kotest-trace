enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
enableFeaturePreview("VERSION_CATALOGS")

rootProject.name = "kotest-trace"

include("kotest-trace-core")
project(":kotest-trace-core").projectDir = file("core")

include("kotest-trace-ktor")
project(":kotest-trace-ktor").projectDir = file("ktor")

include("kotest-trace-example-alert")
project(":kotest-trace-example-alert").projectDir = file("alert")
