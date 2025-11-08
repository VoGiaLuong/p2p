dependencies {
    implementation(project(":modules:file-classification"))
    implementation("org.slf4j:slf4j-api:2.0.12")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
}
