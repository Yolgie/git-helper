buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.25")
    }
}

apply(plugin = "org.jetbrains.kotlin.jvm")
apply(plugin = "application")

repositories {
    mavenCentral()
}

dependencies {
    "testImplementation"(kotlin("test"))
}

application {
    mainClass.set("GitHelperKt")
}

tasks.test {
    useJUnitPlatform()
}
