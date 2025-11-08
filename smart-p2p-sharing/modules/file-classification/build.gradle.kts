dependencies {
    implementation(project(":modules:p2p-transport"))
    implementation("org.slf4j:slf4j-api:2.0.12")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.1")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
}
