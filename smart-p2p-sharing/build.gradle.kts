plugins {
    base
}

allprojects {
    group = "com.example.p2p"
    version = "0.1.0"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")

    configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(17))
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
