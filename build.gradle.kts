plugins {
    id("java-library")
    `maven-publish`
}

group = "app.miyuki"
version = "1.0.6"

java.sourceCompatibility = JavaVersion.VERSION_1_8
java.targetCompatibility = JavaVersion.VERSION_1_8

repositories {
    mavenCentral()
}

dependencies {
    api("org.jetbrains:annotations:23.0.0")
    compileOnly("me.lucko:jar-relocator:1.7")
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
