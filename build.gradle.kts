val kotlin_version: String by project
val logback_version: String by project
val mongo_version: String by project
val ktor_version: String by project

plugins {
    kotlin("jvm") version "2.2.21"
    id("io.ktor.plugin") version "3.3.2"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.21"
}

group = "com.example"
version = "0.0.1"

tasks.withType<Jar> {
    archiveBaseName.set("listlybackend")
}

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

dependencies {
    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-swagger")
    implementation("io.ktor:ktor-server-auth")
    implementation("io.ktor:ktor-server-auth-jwt")
    implementation("io.ktor:ktor-server-default-headers:${ktor_version}")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("io.ktor:ktor-server-cors:${ktor_version}")
    implementation("io.ktor:ktor-server-call-logging:${ktor_version}")
    implementation("org.litote.kmongo:kmongo:4.9.0")
    implementation("io.ktor:ktor-server-netty")
    implementation("io.ktor:ktor-client-content-negotiation:${ktor_version}")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("io.ktor:ktor-server-config-yaml")
    implementation("org.mindrot:jbcrypt:0.4")
    testImplementation("io.ktor:ktor-server-tests-jvm:2.3.4")
    testImplementation("io.mockk:mockk:1.14.4")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.0")
    testImplementation(kotlin("test-junit5"))

    implementation("io.ktor:ktor-server-status-pages:${ktor_version}")

}

tasks.test {
    useJUnitPlatform()
}

tasks.register("testAll") {
    dependsOn("test")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

