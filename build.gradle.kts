plugins {
    kotlin("jvm") version "2.0.21"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.3")
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("GitHelperKt")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
