plugins {
    id("java")
    `maven-publish`
}

group = "app.miyuki"
version = "1.0.5"

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
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            groupId = project.group.toString()
            artifactId = "MiyukiDependencyDownloader"
            version = project.version.toString()
        }
    }
}
