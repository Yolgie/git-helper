plugins {
    kotlin("jvm") version "1.9.25"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

application {
    mainClass.set("GitHelperKt")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
