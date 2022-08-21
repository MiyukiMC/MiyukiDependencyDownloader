plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "7.1.0"
}

group = "app.miyuki"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.projectlombok:lombok:1.18.24")
    annotationProcessor("org.projectlombok:lombok:1.18.24")

    implementation("org.jetbrains:annotations:23.0.0")
}

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
    shadowJar {
        archiveFileName.set("MiyukiDependencyDownloader-${project.version}.jar")
    }
}